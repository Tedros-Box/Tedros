package org.tedros.ai.toolrelay;

import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.tedros.core.security.model.TUser;
import org.tedros.server.util.TLoggerUtil;

import jakarta.enterprise.context.ApplicationScoped;

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

	private void evict(TAiRelayConfigSnapshot cfg) {
		int ttl = cfg.getConversationTtlMin();
		conversations.values().removeIf(c -> {
			boolean expired = c.isIdleExpired(ttl);
			// nao remove conversa em uso (turno rodando neste instante)
			return expired && !c.getLock().isLocked();
		});

		int max = cfg.getMaxConversations();
		while (conversations.size() >= max) {
			TAiConversation lru = conversations.values().stream()
					.filter(c -> !c.getLock().isLocked())
					.min(Comparator.comparingLong(TAiConversation::getLastAccessAt))
					.orElse(null);
			if (lru == null)
				break;
			conversations.remove(lru.getId());
			LOGGER.warn("AI relay conversation cap ({}) reached — evicted LRU conversation {} (user {})",
					max, lru.getId(), lru.getUserId());
		}
	}
}
