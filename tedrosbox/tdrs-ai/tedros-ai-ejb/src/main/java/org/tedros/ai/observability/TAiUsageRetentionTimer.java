package org.tedros.ai.observability;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import org.tedros.core.support.TDomainPropertieSupport;
import org.tedros.server.util.TLoggerUtil;
import org.tedros.server.util.TServiceLocator;

import jakarta.ejb.EJB;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * Timer diario (madrugada) da retencao do consumo de IA: consolida o ledger em
 * {@code TAI_USAGE_MONTHLY} e expurga o detalhe alem da janela
 * {@code sys.ai.usage.retention.months} (default {@value #DEFAULT_RETENTION_MONTHS}).
 * <p>
 * O expurgo e por <b>meses inteiros</b> (boundary = 1º dia do mes N meses atras):
 * nenhum mes fica parcialmente apagado, o que mantem o rollup idempotente
 * (meses ainda presentes recomputam a soma completa; meses apagados congelam no
 * mensal). O rollup roda ANTES do expurgo; falha no rollup aborta o expurgo
 * (nunca se apaga detalhe sem consolidar). Nunca propaga excecao.
 *
 * @author Davis Gordon
 */
@Startup
@Singleton
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class TAiUsageRetentionTimer {

	private static final TLoggerUtil LOGGER = TLoggerUtil.create(TAiUsageRetentionTimer.class);

	static final int DEFAULT_RETENTION_MONTHS = 4;
	static final String PROP_RETENTION_MONTHS = "sys.ai.usage.retention.months";

	@EJB
	TAiUsageRetentionEAO retentionEAO;

	/** Todo dia as 03:20 (fora do horario de pico). */
	@Schedule(hour = "3", minute = "20", persistent = false, info = "ai-usage-retention")
	public void runNightly() {
		try {
			int months = resolveRetentionMonths();
			Date boundary = purgeBoundary(new Date(), months);
			int rolled = retentionEAO.rollup(); // consolida ANTES de apagar
			int[] purged = retentionEAO.purge(boundary);
			LOGGER.info("AI usage retention done: {} monthly rows; purged {} ledger rows and {} events "
					+ "older than {} ({} months)", rolled, purged[0], purged[1], boundary, months);
		} catch (Exception e) {
			// manutencao nunca pode quebrar o container; detalhe preservado ate a proxima
			LOGGER.error("AI usage retention job failed (no purge performed this run)", e);
		}
	}

	/**
	 * Boundary do expurgo: 1º dia do mes, {@code retentionMonths} meses antes de
	 * {@code now}. Apagar {@code ts < boundary} remove apenas meses inteiros.
	 */
	static Date purgeBoundary(Date now, int retentionMonths) {
		LocalDate start = Instant.ofEpochMilli(now.getTime()).atZone(ZoneId.systemDefault()).toLocalDate()
				.withDayOfMonth(1).minusMonths(retentionMonths);
		return Date.from(start.atStartOfDay(ZoneId.systemDefault()).toInstant());
	}

	/** Meses da property, com fallback ao default se ausente/invalida/&lt; 1. */
	static int parseMonths(String value, int def) {
		if (value == null || value.isBlank())
			return def;
		try {
			int n = Integer.parseInt(value.trim());
			return n >= 1 ? n : def;
		} catch (NumberFormatException e) {
			return def;
		}
	}

	private int resolveRetentionMonths() {
		try(TServiceLocator serv = TServiceLocator.getInstance()) {
			TDomainPropertieSupport support = serv.lookupWithRetry(TDomainPropertieSupport.JNDI_NAME);
			return parseMonths(support.getSystemPropertyValue(PROP_RETENTION_MONTHS), DEFAULT_RETENTION_MONTHS);
		} catch (Exception e) {
			LOGGER.warn("Could not read {} — using default {} months: {}",
					PROP_RETENTION_MONTHS, DEFAULT_RETENTION_MONTHS, e.getMessage());
			return DEFAULT_RETENTION_MONTHS;
		} 
	}
}
