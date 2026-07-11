package org.tedros.ai.metrics.web;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * Ativa o JAX-RS neste WAR sem {@code web.xml}. Com o context-root {@code /ai}
 * definido no EAR e o application-path {@code /}, o {@link MetricsEndpoint}
 * responde em {@code /ai/metrics}.
 *
 * @author Davis Gordon
 */
@ApplicationPath("/")
public class MetricsApplication extends Application {
}
