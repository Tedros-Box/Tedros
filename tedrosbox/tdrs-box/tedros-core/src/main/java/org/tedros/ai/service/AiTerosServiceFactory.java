package org.tedros.ai.service;

import org.tedros.ai.service.langchain.LangChainGeminiTerosService;
import org.tedros.ai.service.langchain.LangChainGrokTerosService;
import org.tedros.ai.service.langchain.LangChainOpenAITerosService;
import org.tedros.ai.toolrelay.ToolRelayTerosService;

public class AiTerosServiceFactory {
	
	private static final String PROVIDER_NOT_SUPPORTED = "Provider not supported: ";

	private AiTerosServiceFactory() {
		
	}
	
	/**
	 * Cria o servico no modo Tool Relay: a conversa, o loop de tool calling,
	 * a API key, o modelo e o prompt vivem no backend (EAR tdrs-ai). Ativado
	 * pela property de sistema {@code sys.ai.toolrelay.enabled}.
	 */
	public static IAiTerosService createToolRelay() {
		return ToolRelayTerosService.create();
	}

	public static IAiTerosService newInstanceToolRelay() {
		return ToolRelayTerosService.newInstance();
	}

	public static IAiTerosService createWithLangChain4jAdapters(String apiKey, String aiModel, String assistantPrompt, AiServiceProvider provider) {
		switch (provider) {
		case GROK:
			return LangChainGrokTerosService.create(apiKey, aiModel, assistantPrompt);
		case OPENAI:
			return LangChainOpenAITerosService.create(apiKey, aiModel, assistantPrompt);
		case GEMINI:
			return LangChainGeminiTerosService.create(apiKey, aiModel, assistantPrompt);
		default:
			throw new IllegalArgumentException(PROVIDER_NOT_SUPPORTED + provider);
		}
	}
	
	public static IAiTerosService newInstanceWithLangChain4jAdapters(String apiKey, String aiModel, String assistantPrompt, AiServiceProvider provider) {
		switch (provider) {
		case GROK:
			return LangChainGrokTerosService.newInstance(apiKey, aiModel, assistantPrompt);
		case OPENAI:
			return LangChainOpenAITerosService.newInstance(apiKey, aiModel, assistantPrompt);
		case GEMINI:
			return LangChainGeminiTerosService.newInstance(apiKey, aiModel, assistantPrompt);
		default:
			throw new IllegalArgumentException(PROVIDER_NOT_SUPPORTED + provider);
		}
	}

}
