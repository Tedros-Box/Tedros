package org.tedros.ai.metrics.web;

import org.tedros.ai.observability.TAiMetricsHolder;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Liveness/readiness do modulo de IA em {@code /ai/status}, no formato do
 * MicroProfile Health ({@code status} + {@code checks}). O sinal de prontidao e
 * a inicializacao do registro de metricas (o {@code TAiObservabilityStartup}
 * so publica o registro apos o boot do modulo), o que indica que o EAR de IA
 * subiu. Usado pelo Nginx/orquestrador para reciclar o no.
 * <p>
 * PATH {@code /ai/status} (NAO {@code /ai/health}): o TomEE Plume registra o
 * servlet do MicroProfile Health em {@code <contexto>/health} em cada WAR, com
 * precedencia sobre este recurso JAX-RS — usar {@code /health} causaria colisao.
 * <p>
 * Checagens mais profundas (provider habilitado, ChatModel construivel, banco
 * respondendo) exigem alcancar os beans EJB e podem ser adicionadas via um
 * holder analogo ao {@link TAiMetricsHolder}.
 *
 * @author Davis Gordon
 */
@Path("/status")
public class HealthEndpoint {

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response health() {
		boolean up = TAiMetricsHolder.isReady();
		String body = "{\"status\":\"" + (up ? "UP" : "DOWN") + "\","
				+ "\"checks\":[{\"name\":\"ai-observability\",\"status\":\""
				+ (up ? "UP" : "DOWN") + "\"}]}";
		return Response.status(up ? Response.Status.OK : Response.Status.SERVICE_UNAVAILABLE)
				.entity(body).type(MediaType.APPLICATION_JSON).build();
	}
}
