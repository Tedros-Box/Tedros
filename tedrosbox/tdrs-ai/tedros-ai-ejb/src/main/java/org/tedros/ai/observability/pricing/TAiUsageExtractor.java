package org.tedros.ai.observability.pricing;

import org.tedros.server.util.TLoggerUtil;

import dev.langchain4j.model.googleai.GoogleAiGeminiTokenUsage;
import dev.langchain4j.model.openai.OpenAiTokenUsage;
import dev.langchain4j.model.output.TokenUsage;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Extrai o uso faturavel de UMA resposta do LLM para um {@link TAiCallUsage},
 * fazendo o <i>cast</i> do subtipo de {@link TokenUsage} de cada provedor
 * (OpenAI/Grok expoem cache/reasoning; Gemini expoe cache/thoughts). Ver Parte 3
 * do plano.
 * <p>
 * Invariantes:
 * <ul>
 *   <li>contadores da langchain4j sao {@link Integer} e podem ser {@code null} —
 *       normalizados por {@link #nz(Integer)};</li>
 *   <li>guardas {@code instanceof} evitam {@code ClassCastException} — um
 *       {@link TokenUsage} generico cai no ramo neutro (sem cache/reasoning);</li>
 *   <li>{@code inputUncached} nunca e negativo (cache &gt; input vira 0 + WARN);</li>
 *   <li>OpenAI/Grok: {@code reasoning} JA incluso no output — nao somar;
 *       Gemini: {@code thoughts} cobrados como saida — somar ao output.</li>
 * </ul>
 * Sem estado — CDI para injecao no loop; instanciavel direto nos testes.
 *
 * @author Davis Gordon
 */
@ApplicationScoped
public class TAiUsageExtractor {

	private static final TLoggerUtil LOGGER = TLoggerUtil.create(TAiUsageExtractor.class);

	/** Gemini Pro passa a {@code long_context} acima deste tamanho de prompt. */
	public static final int GEMINI_LONG_CONTEXT_THRESHOLD = 200_000;

	/**
	 * Monta o uso faturavel da chamada. {@code costUsd}/{@code priceVersion}
	 * ficam nulos — sao preenchidos depois pelo {@code TAiPriceBook}.
	 *
	 * @param callIndex indice da chamada no turno (profundidade da recursao)
	 * @param provider  label de baixa cardinalidade (OPENAI/GROK/GEMINI)
	 * @param model     nome exato do modelo ({@code cfg.getModel()})
	 * @param usage     uso retornado pela resposta do LLM (pode ser {@code null})
	 * @param llmMs     latencia da chamada em ms
	 */
	public TAiCallUsage extract(int callIndex, String provider, String model, TokenUsage usage, long llmMs) {
		TAiCallUsage call = new TAiCallUsage();
		call.setCallIndex(callIndex);
		call.setProvider(provider);
		call.setModel(model);
		call.setLlmMs(llmMs);

		if (usage == null) {
			call.setTier(TAiCallUsage.TIER_STANDARD);
			return call; // tudo zero — nao ha o que faturar
		}

		int input = nz(usage.inputTokenCount());
		int output = nz(usage.outputTokenCount());

		if (usage instanceof OpenAiTokenUsage o) {
			// OpenAI e Grok (Grok usa o adapter OpenAI → mesmo subtipo)
			int cached = (o.inputTokensDetails() != null) ? nz(o.inputTokensDetails().cachedTokens()) : 0;
			int reasoning = (o.outputTokensDetails() != null) ? nz(o.outputTokensDetails().reasoningTokens()) : 0;
			call.setInputCached(cached);
			call.setInputUncached(uncached(input, cached, provider, model));
			call.setReasoning(reasoning);
			call.setOutput(output); // reasoning JA incluso no output do provedor — NAO somar
			call.setTier(TAiCallUsage.TIER_STANDARD);
		} else if (usage instanceof GoogleAiGeminiTokenUsage g) {
			int cached = nz(g.cachedContentTokenCount());
			int reasoning = nz(g.thoughtsTokenCount());
			call.setInputCached(cached);
			call.setInputUncached(uncached(input, cached, provider, model));
			call.setReasoning(reasoning);
			call.setOutput(output + reasoning); // thoughts cobrados como saida — SOMAR
			call.setTier(input > GEMINI_LONG_CONTEXT_THRESHOLD
					? TAiCallUsage.TIER_LONG_CONTEXT : TAiCallUsage.TIER_STANDARD);
		} else {
			// TokenUsage generico — sem detalhamento de cache/reasoning
			call.setInputCached(0);
			call.setInputUncached(input);
			call.setReasoning(0);
			call.setOutput(output);
			call.setTier(TAiCallUsage.TIER_STANDARD);
		}
		return call;
	}

	/** {@code input - cached}, nunca negativo: cache &gt; input vira 0 e loga WARN. */
	private long uncached(int input, int cached, String provider, String model) {
		int uncached = input - cached;
		if (uncached < 0) {
			LOGGER.warn("Cached tokens ({}) exceed input tokens ({}) for {}/{} — clamping uncached to 0",
					cached, input, provider, model);
			return 0;
		}
		return uncached;
	}

	/** Normaliza um contador que pode ser {@code null} para 0. */
	private static int nz(Integer value) {
		return value == null ? 0 : value;
	}
}
