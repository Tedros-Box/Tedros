package org.tedros.ai.toolrelay;

import org.tedros.core.security.model.TUser;

/**
 * Contrato do store de conversas do relay. A implementacao da Fase 1
 * ({@link TAiConversationStore}) e em memoria — exige sticky session por
 * conversa em cluster. Uma implementacao distribuida (Redis, padrao
 * {@code SessionCacheProvider} do tedros-core-ejb, ou Mongo) fica para a
 * Fase 2; para serializar as mensagens o langchain4j oferece
 * {@code ChatMessageSerializer}/{@code ChatMessageDeserializer}.
 *
 * @author Davis Gordon
 */
public interface ITAiConversationStore {

	/**
	 * Cria e registra uma conversa nova, aplicando eviction TTL/LRU conforme
	 * a configuracao dada.
	 */
	TAiConversation create(String id, TUser user, TAiRelayConfigSnapshot cfg);

	/** Retorna a conversa ou null se nao existir (ou ja foi removida). */
	TAiConversation get(String id);

	void remove(String id);

	int size();
}
