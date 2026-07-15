/**
 *
 */
package org.tedros.ai.ejb.startup;

/**
 * Properties do modulo AI Tool Relay, cadastradas no boot do EAR
 * pelo {@link AppStartupService}.
 *
 * @author Davis Gordon
 *
 */
public enum AiRelayPropertie {

	TOOL_RELAY_ENABLED("sys.ai.toolrelay.enabled",
			"Enable the AI tool relay mode on the client. Set true or false (default false)"),
	CONVERSATION_TTL_MIN("sys.ai.toolrelay.conversation.ttl.min",
			"Eviction TTL in minutes for inactive relay conversations (default 30)"),
	PENDING_TURN_TTL_MIN("sys.ai.toolrelay.pendingturn.ttl.min",
			"Timeout in minutes for a pending tool call turn (default 5)"),
	MAX_CONVERSATIONS("sys.ai.toolrelay.max.conversations",
			"Global cap of relay conversations kept in memory (default 2000)"),
	DEBUG("sys.ai.toolrelay.debug",
			"Enable LLM request/response logging for the relay. Set true or false (default false)"),
	USAGE_RETENTION_MONTHS("sys.ai.usage.retention.months",
			"Months of detailed AI usage kept (TAI_LLM_CALL/TAI_USAGE_EVENT) before nightly rollup+purge (default 4)"),
	AI_EMBEDDING_PROVIDER ("sys.ai.embedding.provider", "The embedding provider to be called. This is optional only when "
			+ "the AI provider is set to GEMINI or OPENAI, as Grok does not support embeddings.");

	private String value;
	private String description;

	private AiRelayPropertie(String v, String description) {
		this.value = v;
		this.description = description;
	}

	/**
	 * @return the value
	 */
	public String getValue() {
		return value;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}
}
