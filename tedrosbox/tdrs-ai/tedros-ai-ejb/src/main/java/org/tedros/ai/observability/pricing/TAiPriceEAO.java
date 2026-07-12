package org.tedros.ai.observability.pricing;

import java.util.List;

import org.tedros.ai.observability.pricing.entity.TAiPrice;
import org.tedros.core.domain.DomainSchema;
import org.tedros.server.util.TLoggerUtil;

import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Acesso a dados minimo da {@link TAiPrice} sobre a PU {@code tedros_core_pu}.
 * Isola o {@link EntityManager} e as transacoes do container: o
 * {@code TAiPriceBook} ({@code @ApplicationScoped}) cacheia em memoria os precos
 * lidos aqui e o {@code TAiPriceSeed} delega a este EJB a insercao do seed.
 *
 * @author Davis Gordon
 */
@Stateless
public class TAiPriceEAO {

	private static final TLoggerUtil LOGGER = TLoggerUtil.create(TAiPriceEAO.class);

	@PersistenceContext(unitName = "tedros_core_pu")
	private EntityManager em;

	/** Todas as linhas ACTIVE — o {@code TAiPriceBook} resolve tier/vigencia em memoria. */
	public List<TAiPrice> findActive() {
		return em.createQuery("SELECT p FROM TAiPrice p WHERE p.active = true", TAiPrice.class)
				.getResultList();
	}

	/** Total de linhas (idempotencia do seed: so popula quando 0). */
	public long count() {
		Long n = em.createQuery("SELECT COUNT(p) FROM TAiPrice p", Long.class).getSingleResult();
		return n != null ? n : 0L;
	}

	/**
	 * Executa os {@code INSERT}s do seed (uma transacao propria). Cada statement
	 * ja vem sem comentarios e sem {@code ;}; a tabela e qualificada com o schema
	 * {@code tedros_core} (o default da conexao nao e {@code tedros_core}).
	 *
	 * @return numero de linhas inseridas
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public int insertSeed(List<String> statements) {
		if (statements == null || statements.isEmpty())
			return 0;
		int rows = 0;
		for (String stmt : statements) {
			String sql = stmt.replaceFirst("(?i)into\\s+TAI_PRICE",
					"INTO " + DomainSchema.tedros_core + ".TAI_PRICE");
			rows += em.createNativeQuery(sql).executeUpdate();
		}
		LOGGER.info("TAI_PRICE seed inserted {} price rows", rows);
		return rows;
	}
}
