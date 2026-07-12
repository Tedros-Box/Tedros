package org.tedros.ai.observability;

import java.util.List;

import org.tedros.ai.observability.entity.TAiLlmCall;
import org.tedros.ai.observability.entity.TAiUsageEvent;
import org.tedros.server.util.TLoggerUtil;

import jakarta.ejb.Asynchronous;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Persistencia assincrona (fire-and-forget) dos eventos de consumo por turno.
 * O metodo {@link #emit(TAiUsageEvent)} e {@code @Asynchronous}: retorna
 * imediatamente ao {@code interact()} e roda numa thread do pool do container,
 * com transacao propria ({@code REQUIRES_NEW}) sobre a PU {@code tedros_core_pu}.
 * <p>
 * Nunca propaga excecao: uma falha de gravacao de telemetria jamais pode afetar
 * o turno de IA (que ja respondeu ao cliente).
 *
 * @author Davis Gordon
 */
@Stateless
public class TAiUsageEventSink {

	private static final TLoggerUtil LOGGER = TLoggerUtil.create(TAiUsageEventSink.class);

	@PersistenceContext(unitName = "tedros_core_pu")
	private EntityManager em;

	@Asynchronous
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public void emit(TAiUsageEvent event) {
		if (event == null)
			return;
		try {
			em.persist(event);
		} catch (Exception e) {
			LOGGER.error("Failed to persist AI usage event (user " + event.getUserId() + ")", e);
		}
	}

	/**
	 * Persiste o ledger de custo do turno — uma linha por chamada ao LLM.
	 * Assincrono, transacao propria, nunca propaga excecao (billing/telemetria
	 * jamais quebra o turno, que ja respondeu ao cliente).
	 */
	@Asynchronous
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public void emitCalls(List<TAiLlmCall> calls) {
		if (calls == null || calls.isEmpty())
			return;
		try {
			for (TAiLlmCall call : calls)
				em.persist(call);
		} catch (Exception e) {
			LOGGER.error("Failed to persist AI LLM call ledger (" + calls.size() + " rows)", e);
		}
	}
}
