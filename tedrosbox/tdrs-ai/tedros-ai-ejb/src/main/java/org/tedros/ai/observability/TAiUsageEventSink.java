package org.tedros.ai.observability;

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
}
