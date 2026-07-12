package org.tedros.ai.toolrelay;

import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.tedros.ai.observability.TAiMetrics;
import org.tedros.core.security.model.TUser;
import org.tedros.server.util.TLoggerUtil;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Store de conversas em memoria: {@code ConcurrentHashMap} sem lock global.
 * Eviction lazy a cada criacao de conversa: remove as expiradas por TTL de
 * inatividade e, se o cap global for atingido, remove por ultimo acesso
 * (LRU aproximado).
 *
 * @author Davis Gordon
 */
@ApplicationScoped
public class TAiConversationStore implements ITAiConversationStore {

	private static final TLoggerUtil LOGGER = TLoggerUtil.create(TAiConversationStore.class);

	private final Map<String, TAiConversation> conversations = new ConcurrentHashMap<>();

	@Inject
	TAiMetrics metrics;

	@Override
	public TAiConversation create(String id, TUser user, TAiRelayConfigSnapshot cfg) {
		evict(cfg);
		TAiConversation conv = new TAiConversation(id, user);
		conversations.put(id, conv);
		return conv;
	}

	@Override
	public TAiConversation get(String id) {
		return id != null ? conversations.get(id) : null;
	}

	@Override
	public void remove(String id) {
		if (id != null)
			conversations.remove(id);
	}

	@Override
	public int size() {
		return conversations.size();
	}

	@Override
	public int pendingTurnsCount() {
		int count = 0;
		for (TAiConversation c : conversations.values())
			if (c.hasPendingTurn())
				count++;
		return count;
	}

	private void evict(TAiRelayConfigSnapshot cfg) {
		int ttl = cfg.getConversationTtlMin();
		int before = conversations.size();
		conversations.values().removeIf(c -> {
			boolean expired = c.isIdleExpired(ttl);
			// nao remove conversa em uso (turno rodando neste instante)
			return expired && !c.getLock().isLocked();
		});
		if (metrics != null)
			metrics.recordEvictions("ttl", before - conversations.size());

		int max = cfg.getMaxConversations();
		while (conversations.size() >= max) {
			TAiConversation lru = conversations.values().stream()
					.filter(c -> !c.getLock().isLocked())
					.min(Comparator.comparingLong(TAiConversation::getLastAccessAt))
					.orElse(null);
			if (lru == null)
				break;
			conversations.remove(lru.getId());
			if (metrics != null)
				metrics.recordEviction("lru_cap");
			LOGGER.warn("AI relay conversation cap ({}) reached — evicted LRU conversation {} (user {})",
					max, lru.getId(), lru.getUserId());
		}
	}
}
