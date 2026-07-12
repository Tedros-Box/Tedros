package org.tedros.ai.toolrelay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.tedros.ai.model.TAiTurnResponse;
import org.tedros.ai.model.TAiTurnResponseType;
import org.tedros.ai.observability.TAiMetrics;
import org.tedros.ai.observability.pricing.TAiCallUsage;
import org.tedros.ai.observability.pricing.TAiPriceBook;
import org.tedros.ai.observability.pricing.TAiUsageExtractor;
import org.tedros.ai.toolrelay.function.TAiToolContext;
import org.tedros.ai.toolrelay.function.TServerAiFunction;
import org.tedros.ai.toolrelay.function.TServerFunctionCatalog;
import org.tedros.core.security.model.TUser;
import org.tedros.server.security.TAccessToken;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * Testes das metricas da Fase 2 (LLM, tokens e tools) medidas no
 * {@link TAiRelayLoop}, com ChatModel roteirizado e {@link SimpleMeterRegistry}.
 * Confirmam tambem a regra de cardinalidade (sem labels de usuario/conversa).
 */
public class TAiRelayLoopMetricsTest {

	private static final List<String> FORBIDDEN_TAGS = List.of(
			"user", "userid", "user_id", "username", "user_name",
			"conversation", "conversationid", "conversation_id", "apikey", "api_key", "token");

	static class FakeChatModel implements ChatModel {
		final Deque<ChatResponse> responses = new ArrayDeque<>();

		FakeChatModel text(String t) {
			responses.add(resp(AiMessage.from(t)));
			return this;
		}

		FakeChatModel tool(ToolExecutionRequest... r) {
			responses.add(resp(AiMessage.from(r)));
			return this;
		}

		static ChatResponse resp(AiMessage m) {
			return ChatResponse.builder().aiMessage(m).tokenUsage(new TokenUsage(10, 5)).build();
		}

		@Override
		public ChatResponse doChat(ChatRequest request) {
			return responses.poll();
		}
	}

	public static class EmptyModel {
	}

	static class BackendTool implements TServerAiFunction {
		@Override
		public String getName() {
			return "be_tool";
		}

		@Override
		public String getDescription() {
			return "test backend tool";
		}

		@Override
		public Class<?> getModel() {
			return EmptyModel.class;
		}

		@Override
		public Object execute(Object arg, TAiToolContext ctx) {
			return Map.of("status", "success");
		}

		@Override
		public boolean revertToModel() {
			return true;
		}
	}

	private SimpleMeterRegistry reg;
	private TAiRelayLoop loop;
	private TAiConversation conv;
	private TAiToolContext ctx;

	/** Custo fake por chamada (preco > 0) para exercitar {@code recordCost}. */
	private static final BigDecimal FAKE_COST = new BigDecimal("0.0001");

	private TServerFunctionCatalog setup(TServerAiFunction... fns) {
		reg = new SimpleMeterRegistry();
		loop = new TAiRelayLoop();
		loop.metrics = new TAiMetrics(reg); // campo package-private acessivel neste pacote
		loop.extractor = new TAiUsageExtractor();
		// PriceBook fake: custo fixo > 0 por chamada, sem tocar no banco
		loop.priceBook = new TAiPriceBook() {
			@Override
			public BigDecimal cost(TAiCallUsage call) {
				return FAKE_COST;
			}
		};
		TUser user = new TUser("Davis");
		user.setId(1L);
		conv = new TAiConversation("c1", user);
		conv.getMemory().add(UserMessage.from("oi")); // o loop exige memoria nao vazia
		ctx = new TAiToolContext(user, new TAccessToken("token-1"));
		return new TServerFunctionCatalog(List.of(fns));
	}

	private void assertNoHighCardinalityTags() {
		for (Meter meter : reg.getMeters())
			for (Tag tag : meter.getId().getTags())
				assertFalse("Metrica " + meter.getId().getName() + " vazou label: " + tag.getKey(),
						FORBIDDEN_TAGS.contains(tag.getKey().toLowerCase()));
	}

	@Test
	public void tokensAndLlmTimerRecordedPerResponse() {
		TServerFunctionCatalog catalog = setup();
		FakeChatModel model = new FakeChatModel().text("ola");

		TAiTurnResponse resp = loop.run(conv, model, catalog, ctx, "OPENAI", "gpt-x");
		assertEquals(TAiTurnResponseType.FINAL, resp.getType());

		// TokenUsage(10,5) generico → uncached 10, cached 0, output 5, tier standard
		assertEquals(10.0, reg.get("tedros_ai_tokens_total")
				.tag("provider", "OPENAI").tag("model", "gpt-x").tag("tier", "standard")
				.tag("type", "input_uncached").counter().count(), 0.0001);
		assertEquals(0.0, reg.get("tedros_ai_tokens_total")
				.tag("type", "input_cached").counter().count(), 0.0001);
		assertEquals(5.0, reg.get("tedros_ai_tokens_total")
				.tag("type", "output").counter().count(), 0.0001);
		assertEquals(1L, reg.get("tedros_ai_llm_request_seconds")
				.tag("provider", "OPENAI").tag("outcome", "success").timer().count());
		// custo creditado por chamada (preco fake > 0)
		assertEquals(FAKE_COST.doubleValue(), reg.get("tedros_ai_cost_usd_total")
				.tag("provider", "OPENAI").tag("model", "gpt-x").tag("tier", "standard").counter().count(), 1e-9);
		assertNoHighCardinalityTags();
	}

	@Test
	public void backendToolExecutionRecorded() {
		BackendTool tool = new BackendTool();
		TServerFunctionCatalog catalog = setup(tool);
		FakeChatModel model = new FakeChatModel()
				.tool(ToolExecutionRequest.builder().id("c1").name("be_tool").arguments("{}").build())
				.text("done");

		TAiTurnResponse resp = loop.run(conv, model, catalog, ctx, "OPENAI", "gpt-x");
		assertEquals(TAiTurnResponseType.FINAL, resp.getType());

		assertEquals(1.0, reg.get("tedros_ai_tool_calls_total")
				.tag("tool", "be_tool").tag("outcome", "success").counter().count(), 0.0001);
		// duas idas ao LLM => tokens e custo creditados duas vezes (20 uncached, 2x custo)
		assertEquals(20.0, reg.get("tedros_ai_tokens_total")
				.tag("type", "input_uncached").counter().count(), 0.0001);
		assertEquals(2 * FAKE_COST.doubleValue(), reg.get("tedros_ai_cost_usd_total")
				.counter().count(), 1e-9);
		assertNoHighCardinalityTags();
	}

	@Test
	public void unknownToolRecordedAsNotFound() {
		TServerFunctionCatalog catalog = setup();
		FakeChatModel model = new FakeChatModel()
				.tool(ToolExecutionRequest.builder().id("c1").name("ghost_tool").arguments("{}").build());

		loop.run(conv, model, catalog, ctx, "OPENAI", "gpt-x");

		assertEquals(1.0, reg.get("tedros_ai_tool_calls_total")
				.tag("tool", "ghost_tool").tag("outcome", "not_found").counter().count(), 0.0001);
		assertNoHighCardinalityTags();
	}
}
