package org.tedros.ai.observability.pricing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;

import dev.langchain4j.model.googleai.GoogleAiGeminiTokenUsage;
import dev.langchain4j.model.openai.OpenAiTokenUsage;
import dev.langchain4j.model.output.TokenUsage;

/**
 * Testes da extracao de uso faturavel por chamada ({@link TAiUsageExtractor}),
 * cobrindo os 3 providers (OpenAI, Grok via subtipo OpenAI, Gemini), o tiering
 * do Gemini (&gt;200k), reasoning/thoughts e contadores nulos. Ver PR-1 do plano.
 */
public class TAiUsageExtractorTest {

	private TAiUsageExtractor extractor;

	@Before
	public void setUp() {
		extractor = new TAiUsageExtractor();
	}

	private static OpenAiTokenUsage openAi(Integer input, Integer cached, Integer output, Integer reasoning) {
		OpenAiTokenUsage.Builder b = OpenAiTokenUsage.builder()
				.inputTokenCount(input)
				.outputTokenCount(output)
				.totalTokenCount(input != null && output != null ? input + output : null);
		if (cached != null)
			b.inputTokensDetails(OpenAiTokenUsage.InputTokensDetails.builder().cachedTokens(cached).build());
		if (reasoning != null)
			b.outputTokensDetails(OpenAiTokenUsage.OutputTokensDetails.builder().reasoningTokens(reasoning).build());
		return b.build();
	}

	private static GoogleAiGeminiTokenUsage gemini(Integer input, Integer cached, Integer output, Integer thoughts) {
		return GoogleAiGeminiTokenUsage.builder()
				.inputTokenCount(input)
				.outputTokenCount(output)
				.totalTokenCount(input != null && output != null ? input + output : null)
				.cachedContentTokenCount(cached)
				.thoughtsTokenCount(thoughts)
				.build();
	}

	@Test
	public void openAiCacheAndReasoning() {
		TAiCallUsage call = extractor.extract(0, "OPENAI", "gpt-5.1", openAi(100, 30, 50, 10), 1234L);

		assertEquals(0, call.getCallIndex());
		assertEquals("OPENAI", call.getProvider());
		assertEquals("gpt-5.1", call.getModel());
		assertEquals(TAiCallUsage.TIER_STANDARD, call.getTier());
		assertEquals(30L, call.getInputCached());
		assertEquals(70L, call.getInputUncached());
		assertEquals(50L, call.getOutput()); // reasoning NAO somado ao output
		assertEquals(10L, call.getReasoning());
		assertEquals(1234L, call.getLlmMs());
		// precificacao ainda nao aplicada
		assertNull(call.getCostUsd());
		assertNull(call.getPriceVersion());
	}

	@Test
	public void grokUsesOpenAiSubtype() {
		// Grok roda no adapter OpenAI → subtipo OpenAiTokenUsage; so o label muda
		TAiCallUsage call = extractor.extract(2, "GROK", "grok-4-fast-reasoning", openAi(1000, 100, 200, 40), 500L);

		assertEquals("GROK", call.getProvider());
		assertEquals(2, call.getCallIndex());
		assertEquals(100L, call.getInputCached());
		assertEquals(900L, call.getInputUncached());
		assertEquals(200L, call.getOutput());
		assertEquals(40L, call.getReasoning());
		assertEquals(TAiCallUsage.TIER_STANDARD, call.getTier());
	}

	@Test
	public void geminiCacheStandardTier() {
		TAiCallUsage call = extractor.extract(1, "GEMINI", "gemini-2.5-pro", gemini(1000, 200, 100, 40), 800L);

		assertEquals(200L, call.getInputCached());
		assertEquals(800L, call.getInputUncached());
		assertEquals(140L, call.getOutput()); // thoughts (40) SOMADOS ao output (100)
		assertEquals(40L, call.getReasoning());
		assertEquals(TAiCallUsage.TIER_STANDARD, call.getTier());
	}

	@Test
	public void geminiLongContextTierAbove200k() {
		TAiCallUsage call = extractor.extract(0, "GEMINI", "gemini-2.5-pro", gemini(250_000, 0, 100, 0), 900L);

		assertEquals(TAiCallUsage.TIER_LONG_CONTEXT, call.getTier());
		assertEquals(250_000L, call.getInputUncached());
	}

	@Test
	public void geminiExactly200kStaysStandard() {
		// limiar e estrito (> 200k), portanto 200k exato continua standard
		TAiCallUsage call = extractor.extract(0, "GEMINI", "gemini-2.5-pro", gemini(200_000, 0, 100, 0), 0L);
		assertEquals(TAiCallUsage.TIER_STANDARD, call.getTier());
	}

	@Test
	public void openAiNullDetailsAreZeroSafe() {
		// sem inputTokensDetails / outputTokensDetails → cache e reasoning = 0, sem NPE
		TAiCallUsage call = extractor.extract(0, "OPENAI", "gpt-5.1", openAi(100, null, 50, null), 0L);

		assertEquals(0L, call.getInputCached());
		assertEquals(100L, call.getInputUncached());
		assertEquals(50L, call.getOutput());
		assertEquals(0L, call.getReasoning());
	}

	@Test
	public void openAiNullInnerCountersAreZeroSafe() {
		// detalhes presentes, mas com contador interno null
		OpenAiTokenUsage usage = OpenAiTokenUsage.builder()
				.inputTokenCount(null)
				.inputTokensDetails(OpenAiTokenUsage.InputTokensDetails.builder().cachedTokens(null).build())
				.outputTokenCount(null)
				.outputTokensDetails(OpenAiTokenUsage.OutputTokensDetails.builder().reasoningTokens(null).build())
				.build();

		TAiCallUsage call = extractor.extract(0, "OPENAI", "gpt-5.1", usage, 0L);
		assertEquals(0L, call.getInputCached());
		assertEquals(0L, call.getInputUncached());
		assertEquals(0L, call.getOutput());
		assertEquals(0L, call.getReasoning());
	}

	@Test
	public void uncachedNeverNegative() {
		// cache maior que input → uncached clampeado em 0 (com WARN)
		TAiCallUsage call = extractor.extract(0, "OPENAI", "gpt-5.1", openAi(10, 30, 5, 0), 0L);
		assertEquals(0L, call.getInputUncached());
		assertEquals(30L, call.getInputCached());
	}

	@Test
	public void genericTokenUsageFallsBackToNeutralBranch() {
		// TokenUsage puro (sem subtipo) — sem cache/reasoning, tier standard
		TAiCallUsage call = extractor.extract(0, "OPENAI", "gpt-x", new TokenUsage(10, 5), 0L);
		assertEquals(0L, call.getInputCached());
		assertEquals(10L, call.getInputUncached());
		assertEquals(5L, call.getOutput());
		assertEquals(0L, call.getReasoning());
		assertEquals(TAiCallUsage.TIER_STANDARD, call.getTier());
	}

	@Test
	public void nullUsageReturnsZeros() {
		TAiCallUsage call = extractor.extract(3, "OPENAI", "gpt-x", null, 100L);
		assertEquals(3, call.getCallIndex());
		assertEquals(0L, call.getInputUncached());
		assertEquals(0L, call.getInputCached());
		assertEquals(0L, call.getOutput());
		assertEquals(0L, call.getReasoning());
		assertEquals(100L, call.getLlmMs());
		assertEquals(TAiCallUsage.TIER_STANDARD, call.getTier());
	}
}
