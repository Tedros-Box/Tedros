package org.tedros.ai.toolrelay;

/**
 * Snapshot imutavel da configuracao do relay resolvida das properties
 * do sistema. Uma instancia e compartilhada entre turnos ate o TTL do
 * cache do {@link TAiRelayConfig} expirar.
 *
 * @author Davis Gordon
 */
public class TAiRelayConfigSnapshot {

	private final boolean aiEnabled;
	private final TAiProvider provider;
	private final String apiKey;
	private final String model;
	private final String prompt;
	private final int conversationTtlMin;
	private final int pendingTurnTtlMin;
	private final int maxConversations;
	private final boolean debug;

	public TAiRelayConfigSnapshot(boolean aiEnabled, TAiProvider provider, String apiKey,
			String model, String prompt, int conversationTtlMin, int pendingTurnTtlMin,
			int maxConversations, boolean debug) {
		this.aiEnabled = aiEnabled;
		this.provider = provider;
		this.apiKey = apiKey;
		this.model = model;
		this.prompt = prompt;
		this.conversationTtlMin = conversationTtlMin;
		this.pendingTurnTtlMin = pendingTurnTtlMin;
		this.maxConversations = maxConversations;
		this.debug = debug;
	}

	public boolean isAiEnabled() {
		return aiEnabled;
	}

	public TAiProvider getProvider() {
		return provider;
	}

	public String getApiKey() {
		return apiKey;
	}

	public String getModel() {
		return model;
	}

	public String getPrompt() {
		return prompt;
	}

	public int getConversationTtlMin() {
		return conversationTtlMin;
	}

	public int getPendingTurnTtlMin() {
		return pendingTurnTtlMin;
	}

	public int getMaxConversations() {
		return maxConversations;
	}

	public boolean isDebug() {
		return debug;
	}
}
