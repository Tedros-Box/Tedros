package org.tedros.ai.observability;

import org.tedros.ai.toolrelay.TAiConversationStore;
import org.tedros.ai.toolrelay.TAiRelayConfig;
import org.tedros.ai.toolrelay.TRelayModelAdapter;
import org.tedros.server.util.TLoggerUtil;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;

/**
 * Sobe o registro de metricas no boot do EAR (o {@link TAiMetrics} e
 * {@code @ApplicationScoped} e, sozinho, so inicializaria na primeira injecao)
 * e liga os gauges de saude por referencia viva ao store, ao adapter e a
 * configuracao. Os suppliers dos gauges sao avaliados a cada scrape, nunca no
 * boot — portanto nao disparam lookup JNDI durante a inicializacao.
 *
 * @author Davis Gordon
 */
@Startup
@Singleton
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class TAiObservabilityStartup {

	private static final TLoggerUtil LOGGER = TLoggerUtil.create(TAiObservabilityStartup.class);

	@Inject
	TAiMetrics metrics;

	@Inject
	TAiConversationStore store;

	@Inject
	TRelayModelAdapter modelAdapter;

	@Inject
	TAiRelayConfig config;

	@PostConstruct
	void init() {
		try {
			metrics.bindConversationsActive(store::size);
			metrics.bindPendingTurns(store::pendingTurnsCount);
			metrics.bindConversationsMax(() -> config.snapshot().getMaxConversations());
			metrics.bindChatModelCacheSize(modelAdapter::cacheSize);
			LOGGER.info("AI relay observability registry initialized (/ai/metrics ready)");
		} catch (Exception e) {
			// observabilidade nunca pode quebrar o boot do modulo de IA
			LOGGER.error("Error initializing AI relay observability", e);
		}
	}
}
