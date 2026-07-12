package org.tedros.ai.metrics.web;

import org.tedros.ai.observability.TAiMetricsHolder;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

/**
 * Endpoint de scrape do Prometheus para o Tool Relay. Le o texto de exposicao
 * do registro compartilhado do JVM via {@link TAiMetricsHolder} — sem logica de
 * negocio e sem estado.
 * <p>
 * PATH {@code /ai/prometheus} (NAO {@code /ai/metrics}): o TomEE Plume registra
 * automaticamente o servlet do MicroProfile Metrics em {@code <contexto>/metrics}
 * em cada WAR, com precedencia sobre este recurso JAX-RS — usar {@code /metrics}
 * causaria colisao (e, neste build do TomEE, aquele servlet MP ainda esta
 * quebrado). Por isso expomos em {@code /ai/prometheus}.
 * <p>
 * SEGURANCA: nunca deve ficar exposto publicamente (vaza nomes de tools e
 * topologia). O Nginx nao roteia {@code /ai/*}; o Prometheus raspa por dentro da
 * rede Docker ({@code tomeeN:8081/ai/prometheus}).
 *
 * @author Davis Gordon
 */
@Path("/prometheus")
public class MetricsEndpoint {

	private static final String PROMETHEUS_TEXT = "text/plain; version=0.0.4; charset=utf-8";

	@GET
	@Produces(PROMETHEUS_TEXT)
	public Response scrape() {
		if (!TAiMetricsHolder.isReady())
			return Response.status(Response.Status.SERVICE_UNAVAILABLE)
					.entity("# tedros-ai metrics registry not initialized yet\n")
					.type(PROMETHEUS_TEXT).build();
		return Response.ok(TAiMetricsHolder.scrape()).type(PROMETHEUS_TEXT).build();
	}
}
