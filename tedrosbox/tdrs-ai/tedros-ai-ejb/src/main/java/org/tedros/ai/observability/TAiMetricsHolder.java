package org.tedros.ai.observability;

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;

/**
 * Ponte de processo (JVM-wide) entre o modulo EJB, que produz as metricas via
 * {@link TAiMetrics}, e o modulo WAR de exposicao ({@code /ai/metrics}), que
 * apenas le o texto de scrape.
 * <p>
 * Num EAR do TomEE o WAR tem um classloader filho do classloader dos modulos
 * EJB, portanto enxerga esta classe e o registro compartilhado. Este e o unico
 * ponto {@code static} do subsistema de observabilidade — deliberado, para
 * garantir UM registro por JVM independente do escopo CDI de cada archive. A
 * logica de instrumentacao continua em {@link TAiMetrics}, injetavel e testavel.
 *
 * @author Davis Gordon
 */
public final class TAiMetricsHolder {

	private static volatile PrometheusMeterRegistry registry;

	private TAiMetricsHolder() {
	}

	/** Publica o registro Prometheus deste JVM (chamado no boot do {@link TAiMetrics}). */
	static void register(PrometheusMeterRegistry reg) {
		registry = reg;
	}

	/** Texto de exposicao Prometheus, ou string vazia se o registro ainda nao subiu. */
	public static String scrape() {
		PrometheusMeterRegistry reg = registry;
		return reg != null ? reg.scrape() : "";
	}

	/** {@code true} quando o registro ja foi publicado (util para healthcheck). */
	public static boolean isReady() {
		return registry != null;
	}
}
