/**
 *
 */
package org.tedros.ai.domain;

import org.tedros.common.domain.TType;

/**
 * Properties do modulo AI Tool Relay, cadastradas no boot do EAR
 * pelo {@link AppStartupService}.
 *
 * @author Davis Gordon
 *
 */
public enum AiRelayPropertie {
	
	AI_OPENAI_KEY("sys.openai.key","Define the OpenAi Api key", TType.SYSTEM),
	AI_OPENAI_MODEL("sys.openai.model","Define the OpenAi Model", TType.SYSTEM),
	AI_OPENAI_PROMPT("sys.openai.prompt","Define the model system prompt instructions", AiDefaultPrompt.SYSTEM_PROMPT, TType.SYSTEM),
	
	AI_GEMINI_KEY("sys.gemini.key","Define the Gemini Api key", TType.SYSTEM),
	AI_GEMINI_MODEL("sys.gemini.model","Define the Gemini Model", TType.SYSTEM),
	AI_GEMINI_PROMPT("sys.gemini.prompt","Define the model system prompt instructions", AiDefaultPrompt.SYSTEM_PROMPT, TType.SYSTEM),
	
	AI_GROK_KEY("sys.grok.key","Define the Grok Api key", TType.SYSTEM),
	AI_GROK_MODEL("sys.grok.model","Define the Grok Model", TType.SYSTEM),
	AI_GROK_PROMPT("sys.grok.prompt","Define the model system prompt instructions", AiDefaultPrompt.SYSTEM_PROMPT, TType.SYSTEM),
	
	AI_SERVICE_PROVIDER("sys.ai.provider","Define the Ai Service Provider: OPENAI, GROK or GEMINI", TType.SYSTEM),
	AI_ENABLED("sys.ai.enabled","Enable Teros artificial intelligence. Set true or false. Default value: false", "false", TType.SYSTEM),

	AI_TOOL_RELAY_ENABLED("sys.ai.toolrelay.enabled",
			"Enable the AI tool relay mode on the client. Set true or false (default false)", "false", TType.SYSTEM),
	AI_CONVERSATION_TTL_MIN("sys.ai.toolrelay.conversation.ttl.min",
			"Eviction TTL in minutes for inactive relay conversations. Default value: 30", "30" , TType.SYSTEM),
	AI_PENDING_TURN_TTL_MIN("sys.ai.toolrelay.pendingturn.ttl.min",
			"Timeout in minutes for a pending tool call turn. Default value: 5", "5", TType.SYSTEM),
	AI_MAX_CONVERSATIONS("sys.ai.toolrelay.max.conversations",
			"Global cap of relay conversations kept in memory. Default value: 2000", "2000",TType.SYSTEM),
	AI_DEBUG("sys.ai.toolrelay.debug",
			"Enable LLM request/response logging for the relay. Set true or false. Default value: false", "false",TType.SYSTEM),
	AI_USAGE_RETENTION_MONTHS("sys.ai.usage.retention.months",
			"Months of detailed AI usage kept (TAI_LLM_CALL/TAI_USAGE_EVENT) before nightly rollup+purge. Default value: 4", "4", TType.SYSTEM),
	AI_EMBEDDING_PROVIDER ("sys.ai.embedding.provider", "The embedding provider to be called. This is optional only when "
			+ "the AI provider is set to GEMINI or OPENAI, as Grok does not support embeddings.", TType.SYSTEM);

	private String value;
	private String description;
	private String defaultValue;
	private TType type;

	private AiRelayPropertie(String v, String description, String defaultValue, TType type) {
		this.value = v;
		this.description = description;
		this.defaultValue = defaultValue;
		this.type = type;
	}
	
	private AiRelayPropertie(String v, String description, TType type) {
		this.value = v;
		this.description = description;
		this.defaultValue = "";
		this.type = type;
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

	public String getDefaultValue() {
		return defaultValue;
	}

	public TType getType() {
		return type;
	}
}
