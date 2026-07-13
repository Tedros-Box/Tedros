package org.tedros.ai.toolrelay;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.tedros.ai.observability.TAiMetrics;
import org.tedros.server.util.TLoggerUtil;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Adaptacao server-side do {@code LangChainServiceAdapter} do frontend:
 * constroi e cacheia UMA instancia de {@link ChatModel} por configuracao
 * (provider/modelo/chave/debug) — nunca uma por request. Timeout de 120s;
 * logs de request/response desligados por default ({@code sys.ai.toolrelay.debug}).
 * <p>
 * GROK usa o adapter OpenAI com baseUrl customizada (mesmo padrao do
 * {@code LangChainGrokTerosService} do FE); GEMINI usa o modulo
 * langchain4j-google-ai-gemini.
 *
 * @author Davis Gordon
 */
@ApplicationScoped
public class TRelayModelAdapter {

	private static final TLoggerUtil LOGGER = TLoggerUtil.create(TRelayModelAdapter.class);

	private static final String GROK_BASE_URL = "https://api.x.ai/v1";
	private static final Duration TIMEOUT = Duration.ofSeconds(120);

	private final Map<String, ChatModel> cache = new ConcurrentHashMap<>();

	@Inject
	TAiMetrics metrics;

	/**
	 * Retorna o ChatModel para a configuracao dada, criando-o na primeira
	 * chamada. Configuracoes antigas sao descartadas quando a configuracao
	 * muda (o cache guarda no maximo algumas entradas).
	 */
	public ChatModel modelFor(TAiRelayConfigSnapshot cfg) {
		String key = cacheKey(cfg);
		return cache.computeIfAbsent(key, k -> {
			if (cache.size() > 4) {
				cache.clear();
				if (metrics != null)
					metrics.recordChatModelCacheClear();
				LOGGER.info("AI relay ChatModel cache cleared after configuration change");
			}
			return build(cfg);
		});
	}

	/** Tamanho atual do cache de ChatModel (para o gauge de observabilidade). */
	public int cacheSize() {
		return cache.size();
	}

	private String cacheKey(TAiRelayConfigSnapshot cfg) {
		return cfg.getProvider() + "|" + cfg.getModel() + "|"
				+ (cfg.getApiKey() != null ? cfg.getApiKey().hashCode() : 0) + "|" + cfg.isDebug();
	}

	private ChatModel build(TAiRelayConfigSnapshot cfg) {
		LOGGER.info("Building ChatModel for provider {} model {}", cfg.getProvider(), cfg.getModel());
		switch (cfg.getProvider()) {
		case OPENAI:
			return openAi(cfg, null);
		case GROK:
			return openAi(cfg, GROK_BASE_URL);
		case GEMINI:
			// returnThinking/sendThinking sao DESABILITADOS por padrao no langchain4j:
			// sem eles a assinatura de raciocinio (thoughtSignature) dos modelos Gemini
			// 2.5+ nao e armazenada em AiMessage.attributes() nem reenviada nos turnos
			// seguintes. Como o loop faz tool calling multi-turno, a ausencia da
			// assinatura faz a API rejeitar o proximo turno com
			// "function call turn must come immediately after a user turn or after a
			// function response turn". OpenAI/GROK nao usam esse mecanismo.
			return GoogleAiGeminiChatModel.builder()
					.apiKey(cfg.getApiKey())
					.modelName(cfg.getModel())
					.timeout(TIMEOUT)
					.returnThinking(true)
					.sendThinking(true)
					.logRequestsAndResponses(cfg.isDebug())
					.build();
		default:
			throw new IllegalArgumentException("Provider not supported: " + cfg.getProvider());
		}
	}

	private ChatModel openAi(TAiRelayConfigSnapshot cfg, String baseUrl) {
		OpenAiChatModel.OpenAiChatModelBuilder builder = OpenAiChatModel.builder()
				.apiKey(cfg.getApiKey())
				.modelName(cfg.getModel())
				.timeout(TIMEOUT)
				.logRequests(cfg.isDebug())
				.logResponses(cfg.isDebug());
		if (baseUrl != null)
			builder.baseUrl(baseUrl);
		return builder.build();
	}
}
