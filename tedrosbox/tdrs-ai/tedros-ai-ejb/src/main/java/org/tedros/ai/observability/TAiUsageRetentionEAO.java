package org.tedros.ai.observability;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.tedros.ai.observability.entity.TAiUsageMonthly;
import org.tedros.server.util.TLoggerUtil;

import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Operacoes transacionais da retencao do consumo de IA sobre a PU
 * {@code tedros_core_pu}: rollup mensal idempotente do ledger e expurgo do
 * detalhe. Chamado pelo {@code TAiUsageRetentionTimer} (que decide a janela).
 * <p>
 * Cada metodo abre a propria transacao ({@code REQUIRES_NEW}): o rollup roda
 * ANTES do expurgo — se o rollup falhar, o expurgo nao ocorre (o detalhe nunca
 * e apagado sem estar consolidado no mensal).
 *
 * @author Davis Gordon
 */
@Stateless
public class TAiUsageRetentionEAO {

	private static final TLoggerUtil LOGGER = TLoggerUtil.create(TAiUsageRetentionEAO.class);

	@PersistenceContext(unitName = "tedros_core_pu")
	private EntityManager em;

	/**
	 * Consolida o ledger em {@code TAI_USAGE_MONTHLY} agrupando por
	 * {@code (user_id, provider, model, ano, mes)}. Idempotente: recomputa a soma
	 * do ledger atual e faz upsert pela chave logica. Meses ja expurgados nao
	 * aparecem no agrupamento, portanto seus totais no mensal ficam congelados.
	 *
	 * @return numero de grupos (linhas mensais) consolidados
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public int rollup() {
		List<Object[]> groups = em.createQuery(
				"SELECT c.userId, c.provider, c.model, "
						+ "EXTRACT(YEAR FROM c.ts), EXTRACT(MONTH FROM c.ts), "
						+ "SUM(c.tokensInFull), SUM(c.tokensInCache), SUM(c.tokensOut), SUM(c.costUsd) "
						+ "FROM TAiLlmCall c "
						+ "WHERE c.userId IS NOT NULL AND c.provider IS NOT NULL "
						+ "AND c.model IS NOT NULL AND c.ts IS NOT NULL "
						+ "GROUP BY c.userId, c.provider, c.model, "
						+ "EXTRACT(YEAR FROM c.ts), EXTRACT(MONTH FROM c.ts)",
				Object[].class).getResultList();

		for (Object[] g : groups) {
			Long userId = (Long) g[0];
			String provider = (String) g[1];
			String model = (String) g[2];
			int year = ((Number) g[3]).intValue();
			int month = ((Number) g[4]).intValue();
			String yearMonth = String.format("%04d-%02d", year, month);
			upsert(userId, provider, model, yearMonth,
					lng(g[5]), lng(g[6]), lng(g[7]), (BigDecimal) g[8]);
		}
		return groups.size();
	}

	private void upsert(Long userId, String provider, String model, String yearMonth,
			long sumInFull, long sumInCache, long sumOut, BigDecimal sumCost) {
		List<TAiUsageMonthly> found = em.createQuery(
				"SELECT m FROM TAiUsageMonthly m WHERE m.userId = :u AND m.provider = :p "
						+ "AND m.model = :mo AND m.yearMonth = :ym",
				TAiUsageMonthly.class)
				.setParameter("u", userId).setParameter("p", provider)
				.setParameter("mo", model).setParameter("ym", yearMonth)
				.getResultList();

		boolean isNew = found.isEmpty();
		TAiUsageMonthly m = isNew ? new TAiUsageMonthly() : found.get(0);
		m.setUserId(userId);
		m.setProvider(provider);
		m.setModel(model);
		m.setYearMonth(yearMonth);
		m.setSumInFull(sumInFull);
		m.setSumInCache(sumInCache);
		m.setSumOut(sumOut);
		m.setSumCostUsd(sumCost);
		if (isNew)
			em.persist(m); // linha existente ja e gerenciada — flush no commit
	}

	/**
	 * Apaga o detalhe anterior a {@code boundary} (meses inteiros — nunca
	 * parcial). {@code TAI_USAGE_MONTHLY} nao e tocado (retencao longa).
	 *
	 * @return {@code [linhas de TAI_LLM_CALL, linhas de TAI_USAGE_EVENT]} apagadas
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public int[] purge(Date boundary) {
		int calls = em.createQuery("DELETE FROM TAiLlmCall c WHERE c.ts < :b")
				.setParameter("b", boundary).executeUpdate();
		int events = em.createQuery("DELETE FROM TAiUsageEvent e WHERE e.ts < :b")
				.setParameter("b", boundary).executeUpdate();
		LOGGER.info("AI usage purge older than {}: {} ledger rows, {} usage events", boundary, calls, events);
		return new int[] { calls, events };
	}

	private static long lng(Object v) {
		return v instanceof Number n ? n.longValue() : 0L;
	}
}
