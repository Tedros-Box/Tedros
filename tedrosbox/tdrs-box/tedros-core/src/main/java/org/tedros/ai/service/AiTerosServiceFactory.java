package org.tedros.ai.service;

import org.slf4j.Logger;
import org.tedros.ai.service.langchain.LangChainGeminiTerosService;
import org.tedros.ai.service.langchain.LangChainGrokTerosService;
import org.tedros.ai.service.langchain.LangChainOpenAITerosService;
import org.tedros.ai.toolrelay.ToolRelayTerosService;
import org.tedros.core.context.TedrosContext;
import org.tedros.core.controller.TPropertieController;
import org.tedros.core.service.remote.TEjbServiceLocator;
import org.tedros.server.result.TResult;
import org.tedros.server.result.TResult.TState;
import org.tedros.util.TLoggerUtil;

class AiTerosServiceFactory {
	
	private static final Logger LOGGER = TLoggerUtil.getLogger(AiTerosServiceFactory.class);
	
	private static final String PROVIDER_NOT_SUPPORTED = "Provider not supported: ";
	
	private static AiTerosServiceFactory instance;

	private AiTerosServiceFactory() {		
	}
	
	public synchronized static AiTerosServiceFactory instance() {
		if(instance==null)
			instance = new AiTerosServiceFactory();		
		return instance;
	}
	
	public boolean isToolRelayEnabled() {
		try (TEjbServiceLocator loc = TEjbServiceLocator.getInstance()) {
			TPropertieController serv = loc.lookup(TPropertieController.JNDI_NAME);
			TResult<String> res = serv.getValue(TedrosContext.getLoggedUser().getAccessToken(),
					"sys.ai.toolrelay.enabled");
			return TState.SUCCESS.equals(res.getState()) && Boolean.parseBoolean(res.getValue());
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			return false;
		}
	}
	
	/**
	 * Cria o servico no modo Tool Relay: a conversa, o loop de tool calling,
	 * a API key, o modelo e o prompt vivem no backend (EAR tdrs-ai). Ativado
	 * pela property de sistema {@code sys.ai.toolrelay.enabled}.
	 */
	public IAiTerosService createToolRelay() {
		return ToolRelayTerosService.create();
	}

	public IAiTerosService newInstanceToolRelay() {
		return ToolRelayTerosService.newInstance();
	}

	public IAiTerosService createWithLangChain4jAdapters(String apiKey, String aiModel, String assistantPrompt, AiServiceProvider provider) {
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
	
	public IAiTerosService newInstanceWithLangChain4jAdapters(String apiKey, String aiModel, String assistantPrompt, AiServiceProvider provider) {
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
