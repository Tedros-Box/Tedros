package org.tedros.ai.ejb.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.tedros.ai.model.TAiClientToolResult;
import org.tedros.ai.model.TAiClientToolSpec;
import org.tedros.ai.model.TAiTurnRequest;
import org.tedros.ai.model.TAiTurnRequestType;
import org.tedros.ai.model.TAiTurnResponse;
import org.tedros.ai.model.TAiTurnResponseType;
import org.tedros.ai.toolrelay.TAiConversation;
import org.tedros.ai.toolrelay.TAiConversationStore;
import org.tedros.ai.toolrelay.TAiProvider;
import org.tedros.ai.toolrelay.TAiRelayConfig;
import org.tedros.ai.toolrelay.TAiRelayConfigSnapshot;
import org.tedros.ai.toolrelay.TAiRelayLoop;
import org.tedros.ai.toolrelay.TRelayModelAdapter;
import org.tedros.ai.toolrelay.function.TAiToolContext;
import org.tedros.ai.toolrelay.function.TServerAiFunction;
import org.tedros.ai.toolrelay.function.TServerFunctionCatalog;
import org.tedros.core.security.model.TUser;
import org.tedros.server.ejb.controller.ITSecurityController;
import org.tedros.server.entity.ITUser;
import org.tedros.server.security.TAccessToken;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;

/**
 * Testes do protocolo do Tool Relay com ChatModel mockado (sem chamadas
 * externas): turno sem tools; tool so de BE; tool so de FE
 * (suspensao/retomada); tools mistas; revertToModel=false; TURN_IN_PROGRESS;
 * timeout de turno pendente; MAX_DEPTH; conversas concorrentes.
 */
public class TAiToolRelayServiceTest {

	private static final String FE_TOOL = "open_view";
	private static final String BE_TOOL = "get_server_datetime_test";

	// ---------------------------------------------------------------- fakes

	/** ChatModel roteirizado: devolve as respostas na ordem enfileirada. */
	static class FakeChatModel implements ChatModel {
		final Deque<ChatResponse> responses = new ArrayDeque<>();
		final List<ChatRequest> requests = new ArrayList<>();
		final AtomicInteger calls = new AtomicInteger();

		FakeChatModel enqueueText(String text) {
			responses.add(response(AiMessage.from(text)));
			return this;
		}

		FakeChatModel enqueueToolCalls(ToolExecutionRequest... reqs) {
			responses.add(response(AiMessage.from(reqs)));
			return this;
		}

		static ChatResponse response(AiMessage message) {
			return ChatResponse.builder().aiMessage(message)
					.tokenUsage(new TokenUsage(10, 5)).build();
		}

		@Override
		public ChatResponse doChat(ChatRequest request) {
			requests.add(request);
			calls.incrementAndGet();
			if (responses.isEmpty())
				throw new IllegalStateException("No scripted response left");
			return responses.poll();
		}
	}

	/** ChatModel que ecoa a ultima mensagem do usuario (para concorrencia). */
	static class EchoChatModel implements ChatModel {
		@Override
		public ChatResponse doChat(ChatRequest request) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			String lastUserText = "";
			for (ChatMessage m : request.messages())
				if (m instanceof UserMessage um)
					lastUserText = um.singleText();
			return FakeChatModel.response(AiMessage.from("echo:" + lastUserText));
		}
	}

	static class FakeSecurityController implements ITSecurityController {
		final Map<String, TUser> usersByToken = new ConcurrentHashMap<>();

		@Override
		public ITUser getUser(TAccessToken token) {
			return usersByToken.get(token.getToken());
		}

		@Override
		public boolean isAccessGranted(TAccessToken clent) {
			return true;
		}

		@Override
		public boolean isPolicieAllowed(TAccessToken token, String securityId, String... action) {
			return true;
		}
	}

	static class FakeConfig extends TAiRelayConfig {
		TAiRelayConfigSnapshot snap;

		@Override
		public TAiRelayConfigSnapshot snapshot() {
			return snap;
		}
	}

	static class FakeAdapter extends TRelayModelAdapter {
		ChatModel model;

		@Override
		public ChatModel modelFor(TAiRelayConfigSnapshot cfg) {
			return model;
		}
	}

	public static class EmptyModel {

	}

	static class BackendTool implements TServerAiFunction {
		final boolean revert;
		final AtomicInteger executions = new AtomicInteger();
		volatile TUser lastUser;
		volatile TAccessToken lastToken;

		BackendTool(boolean revert) {
			this.revert = revert;
		}

		@Override
		public String getName() {
			return BE_TOOL;
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
			executions.incrementAndGet();
			lastUser = ctx.getUser();
			lastToken = ctx.getToken();
			return Map.of("status", "success");
		}

		@Override
		public boolean revertToModel() {
			return revert;
		}
	}

	// ---------------------------------------------------------------- setup

	private final FakeSecurityController security = new FakeSecurityController();
	private final TAccessToken token = new TAccessToken("token-1");

	private TAiToolRelayService service(ChatModel model, int pendingTurnTtlMin, TServerAiFunction... fns) {
		TUser user = new TUser("Davis");
		user.setId(1L);
		security.usersByToken.put("token-1", user);

		TAiToolRelayService s = new TAiToolRelayService();
		s.securityController = security;
		FakeConfig cfg = new FakeConfig();
		cfg.snap = snapshot(true, pendingTurnTtlMin);
		s.config = cfg;
		FakeAdapter adapter = new FakeAdapter();
		adapter.model = model;
		s.modelAdapter = adapter;
		s.catalog = new TServerFunctionCatalog(List.of(fns));
		s.store = new TAiConversationStore();
		s.loop = new TAiRelayLoop();
		return s;
	}

	private static TAiRelayConfigSnapshot snapshot(boolean enabled, int pendingTurnTtlMin) {
		return new TAiRelayConfigSnapshot(enabled, TAiProvider.OPENAI, "key", "model",
				"You are Teros.", 30, pendingTurnTtlMin, 2000, false);
	}

	private static TAiTurnRequest message(String conversationId, String text, List<TAiClientToolSpec> specs) {
		TAiTurnRequest req = new TAiTurnRequest();
		req.setType(TAiTurnRequestType.MESSAGE);
		req.setConversationId(conversationId);
		req.setUserMessage(text);
		req.setClientToolSpecs(specs);
		return req;
	}

	private static TAiTurnRequest toolResults(String conversationId, TAiClientToolResult... results) {
		TAiTurnRequest req = new TAiTurnRequest();
		req.setType(TAiTurnRequestType.TOOL_RESULTS);
		req.setConversationId(conversationId);
		req.setToolResults(List.of(results));
		return req;
	}

	private static TAiClientToolResult clientResult(String callId, boolean revert) {
		TAiClientToolResult r = new TAiClientToolResult();
		r.setCallId(callId);
		r.setName(FE_TOOL);
		r.setResultJson("{\"status\":\"success\"}");
		r.setRevertToModel(revert);
		return r;
	}

	private static List<TAiClientToolSpec> clientSpecs() {
		TAiClientToolSpec spec = new TAiClientToolSpec();
		spec.setName(FE_TOOL);
		spec.setDescription("Opens a view");
		spec.setParametersSchemaJson(
				"{\"type\":\"object\",\"properties\":{\"viewPath\":{\"type\":\"string\"}},\"required\":[\"viewPath\"]}");
		return List.of(spec);
	}

	private static ToolExecutionRequest feCall(String id) {
		return ToolExecutionRequest.builder().id(id).name(FE_TOOL)
				.arguments("{\"viewPath\":\"module/view\"}").build();
	}

	private static ToolExecutionRequest beCall(String id) {
		return ToolExecutionRequest.builder().id(id).name(BE_TOOL).arguments("{}").build();
	}

	// ---------------------------------------------------------------- tests

	@Test
	public void turnWithoutTools() {
		FakeChatModel model = new FakeChatModel().enqueueText("Ola!");
		TAiToolRelayService s = service(model, 5);

		TAiTurnResponse resp = s.interact(token, message(null, "Oi", clientSpecs()));

		assertEquals(TAiTurnResponseType.FINAL, resp.getType());
		assertEquals("Ola!", resp.getText());
		assertNotNull(resp.getConversationId());
		assertEquals(1, model.calls.get());
		assertNotNull(resp.getTokenUsage());
		assertEquals(Long.valueOf(15), resp.getTokenUsage().getTotal());
	}

	@Test
	public void backendToolOnly() {
		FakeChatModel model = new FakeChatModel()
				.enqueueToolCalls(beCall("c1"))
				.enqueueText("done");
		BackendTool tool = new BackendTool(true);
		TAiToolRelayService s = service(model, 5, tool);

		TAiTurnResponse resp = s.interact(token, message(null, "que horas sao?", null));

		assertEquals(TAiTurnResponseType.FINAL, resp.getType());
		assertEquals("done", resp.getText());
		assertEquals(2, model.calls.get());
		assertEquals(1, tool.executions.get());
		assertNotNull(tool.lastUser);
		assertEquals(Long.valueOf(1), tool.lastUser.getId());
		// token do request corrente propagado a tool de BE
		assertNotNull(tool.lastToken);
		assertEquals("token-1", tool.lastToken.getToken());
		// tokens acumulados das duas idas ao LLM
		assertEquals(Long.valueOf(30), resp.getTokenUsage().getTotal());
	}

	@Test
	public void frontendToolSuspendAndResume() {
		FakeChatModel model = new FakeChatModel()
				.enqueueToolCalls(feCall("c1"))
				.enqueueText("view opened");
		TAiToolRelayService s = service(model, 5);

		TAiTurnResponse r1 = s.interact(token, message(null, "abra a tela", clientSpecs()));
		assertEquals(TAiTurnResponseType.CLIENT_TOOL_CALLS, r1.getType());
		assertEquals(1, r1.getPendingCalls().size());
		assertEquals("c1", r1.getPendingCalls().get(0).getCallId());
		assertEquals(FE_TOOL, r1.getPendingCalls().get(0).getName());
		assertEquals(1, model.calls.get());

		TAiTurnResponse r2 = s.interact(token, toolResults(r1.getConversationId(), clientResult("c1", true)));
		assertEquals(TAiTurnResponseType.FINAL, r2.getType());
		assertEquals("view opened", r2.getText());
		assertEquals(2, model.calls.get());
	}

	@Test
	public void mixedToolsKeepMemoryOrder() {
		FakeChatModel model = new FakeChatModel()
				.enqueueToolCalls(beCall("be1"), feCall("fe1"))
				.enqueueText("all done");
		BackendTool tool = new BackendTool(true);
		TAiToolRelayService s = service(model, 5, tool);

		TAiTurnResponse r1 = s.interact(token, message(null, "faca tudo", clientSpecs()));
		assertEquals(TAiTurnResponseType.CLIENT_TOOL_CALLS, r1.getType());
		// somente a call de FE volta pendente
		assertEquals(1, r1.getPendingCalls().size());
		assertEquals("fe1", r1.getPendingCalls().get(0).getCallId());
		assertEquals(1, tool.executions.get());

		TAiTurnResponse r2 = s.interact(token, toolResults(r1.getConversationId(), clientResult("fe1", true)));
		assertEquals(TAiTurnResponseType.FINAL, r2.getType());
		assertEquals("all done", r2.getText());

		// memoria: ... AiMessage(tool calls), result(be1), result(fe1), AiMessage final
		TAiConversation conv = s.store.get(r1.getConversationId());
		List<ChatMessage> messages = conv.getMemory().messages();
		int n = messages.size();
		assertTrue(messages.get(n - 1) instanceof AiMessage);
		ToolExecutionResultMessage beResult = (ToolExecutionResultMessage) messages.get(n - 3);
		ToolExecutionResultMessage feResult = (ToolExecutionResultMessage) messages.get(n - 2);
		assertEquals("be1", beResult.id());
		assertEquals("fe1", feResult.id());
	}

	@Test
	public void revertToModelFalseEndsWithoutNewLlmCall() {
		FakeChatModel model = new FakeChatModel();
		model.responses.add(FakeChatModel.response(new AiMessage("Opening the view", List.of(feCall("c1")))));
		TAiToolRelayService s = service(model, 5);

		TAiTurnResponse r1 = s.interact(token, message(null, "abra a tela", clientSpecs()));
		assertEquals(TAiTurnResponseType.CLIENT_TOOL_CALLS, r1.getType());

		TAiTurnResponse r2 = s.interact(token, toolResults(r1.getConversationId(), clientResult("c1", false)));
		assertEquals(TAiTurnResponseType.FINAL, r2.getType());
		assertEquals("Opening the view", r2.getText());
		// nenhuma nova ida ao LLM
		assertEquals(1, model.calls.get());
		// os results entraram na memoria mesmo assim
		TAiConversation conv = s.store.get(r1.getConversationId());
		List<ChatMessage> messages = conv.getMemory().messages();
		assertTrue(messages.get(messages.size() - 1) instanceof ToolExecutionResultMessage);
	}

	@Test
	public void secondMessageWhileTurnPendingReturnsTurnInProgress() {
		FakeChatModel model = new FakeChatModel().enqueueToolCalls(feCall("c1"));
		TAiToolRelayService s = service(model, 5);

		TAiTurnResponse r1 = s.interact(token, message(null, "abra a tela", clientSpecs()));
		assertEquals(TAiTurnResponseType.CLIENT_TOOL_CALLS, r1.getType());

		TAiTurnResponse r2 = s.interact(token, message(r1.getConversationId(), "oi de novo", null));
		assertEquals(TAiTurnResponseType.ERROR, r2.getType());
		assertEquals(TAiToolRelayService.ERROR_TURN_IN_PROGRESS, r2.getErrorCode());
	}

	@Test
	public void expiredPendingTurnIsSanitizedOnNextMessage() throws Exception {
		FakeChatModel model = new FakeChatModel()
				.enqueueToolCalls(feCall("c1"))
				.enqueueText("recovered");
		// TTL zero: o turno pendente expira imediatamente
		TAiToolRelayService s = service(model, 0);

		TAiTurnResponse r1 = s.interact(token, message(null, "abra a tela", clientSpecs()));
		assertEquals(TAiTurnResponseType.CLIENT_TOOL_CALLS, r1.getType());
		Thread.sleep(10);

		TAiTurnResponse r2 = s.interact(token, message(r1.getConversationId(), "continua", null));
		assertEquals(TAiTurnResponseType.FINAL, r2.getType());
		assertEquals("recovered", r2.getText());

		// a call orfa recebeu result sintetico cancelled
		TAiConversation conv = s.store.get(r1.getConversationId());
		boolean cancelled = conv.getMemory().messages().stream()
				.filter(m -> m instanceof ToolExecutionResultMessage)
				.map(m -> ((ToolExecutionResultMessage) m).text())
				.anyMatch(t -> t.contains("cancelled"));
		assertTrue(cancelled);
	}

	@Test
	public void toolResultsAfterExpiryReturnsNoPendingTurn() throws Exception {
		FakeChatModel model = new FakeChatModel().enqueueToolCalls(feCall("c1"));
		TAiToolRelayService s = service(model, 0);

		TAiTurnResponse r1 = s.interact(token, message(null, "abra a tela", clientSpecs()));
		Thread.sleep(10);

		TAiTurnResponse r2 = s.interact(token, toolResults(r1.getConversationId(), clientResult("c1", true)));
		assertEquals(TAiTurnResponseType.ERROR, r2.getType());
		assertEquals(TAiToolRelayService.ERROR_NO_PENDING_TURN, r2.getErrorCode());
	}

	@Test
	public void maxDepthIsEnforcedPerTurn() {
		FakeChatModel model = new FakeChatModel();
		for (int i = 0; i < TAiRelayLoop.MAX_RECURSION_DEPTH + 1; i++)
			model.enqueueToolCalls(beCall("c" + i));
		BackendTool tool = new BackendTool(true);
		TAiToolRelayService s = service(model, 5, tool);

		TAiTurnResponse resp = s.interact(token, message(null, "loop!", null));
		assertEquals(TAiTurnResponseType.ERROR, resp.getType());
		assertEquals(TAiRelayLoop.ERROR_MAX_DEPTH, resp.getErrorCode());
		assertEquals(TAiRelayLoop.MAX_RECURSION_DEPTH, model.calls.get());
	}

	@Test
	public void aiDisabledReturnsErrorWithoutLlmCall() {
		FakeChatModel model = new FakeChatModel().enqueueText("nunca");
		TAiToolRelayService s = service(model, 5);
		((FakeConfig) s.config).snap = new TAiRelayConfigSnapshot(false, null, null, null, null, 30, 5, 2000, false);

		TAiTurnResponse resp = s.interact(token, message(null, "oi", null));
		assertEquals(TAiTurnResponseType.ERROR, resp.getType());
		assertEquals(TAiToolRelayService.ERROR_AI_DISABLED, resp.getErrorCode());
		assertEquals(0, model.calls.get());
	}

	@Test
	public void toolResultsWithWrongCallIdsAreRejected() {
		FakeChatModel model = new FakeChatModel().enqueueToolCalls(feCall("c1"));
		TAiToolRelayService s = service(model, 5);

		TAiTurnResponse r1 = s.interact(token, message(null, "abra a tela", clientSpecs()));
		TAiTurnResponse r2 = s.interact(token, toolResults(r1.getConversationId(), clientResult("wrong-id", true)));

		assertEquals(TAiTurnResponseType.ERROR, r2.getType());
		assertEquals(TAiToolRelayService.ERROR_INVALID_TOOL_RESULTS, r2.getErrorCode());
	}

	@Test
	public void unknownConversationAndForeignUserAreRejected() {
		FakeChatModel model = new FakeChatModel().enqueueText("Ola!");
		TAiToolRelayService s = service(model, 5);

		TAiTurnResponse r0 = s.interact(token, message("nao-existe", "oi", null));
		assertEquals(TAiTurnResponseType.ERROR, r0.getType());
		assertEquals(TAiToolRelayService.ERROR_CONVERSATION_NOT_FOUND, r0.getErrorCode());

		TAiTurnResponse r1 = s.interact(token, message(null, "oi", null));
		assertEquals(TAiTurnResponseType.FINAL, r1.getType());

		// outro usuario nao acessa a conversa
		TUser other = new TUser("Intruso");
		other.setId(99L);
		security.usersByToken.put("token-2", other);
		TAiTurnResponse r2 = s.interact(new TAccessToken("token-2"),
				message(r1.getConversationId(), "oi", null));
		assertEquals(TAiTurnResponseType.ERROR, r2.getType());
		assertEquals(TAiToolRelayService.ERROR_CONVERSATION_NOT_FOUND, r2.getErrorCode());
	}

	@Test
	public void closeDiscardsConversation() {
		FakeChatModel model = new FakeChatModel().enqueueText("Ola!");
		TAiToolRelayService s = service(model, 5);

		TAiTurnResponse r1 = s.interact(token, message(null, "oi", null));
		String id = r1.getConversationId();
		assertNotNull(s.store.get(id));

		TAiTurnRequest close = new TAiTurnRequest();
		close.setType(TAiTurnRequestType.CLOSE);
		close.setConversationId(id);
		TAiTurnResponse r2 = s.interact(token, close);
		assertEquals(TAiTurnResponseType.FINAL, r2.getType());
		assertNull(s.store.get(id));
	}

	@Test
	public void concurrentConversationsOfDifferentUsersDoNotInterfere() throws Exception {
		TAiToolRelayService s = service(new EchoChatModel(), 5);
		TUser user2 = new TUser("Maria");
		user2.setId(2L);
		security.usersByToken.put("token-2", user2);

		int threads = 8;
		CountDownLatch latch = new CountDownLatch(threads);
		List<Throwable> failures = new ArrayList<>();
		List<Thread> pool = new ArrayList<>();
		for (int i = 0; i < threads; i++) {
			final int idx = i;
			Thread t = new Thread(() -> {
				try {
					TAccessToken tk = new TAccessToken(idx % 2 == 0 ? "token-1" : "token-2");
					String msg = "mensagem-" + idx;
					TAiTurnResponse resp = s.interact(tk, message(null, msg, null));
					assertEquals(TAiTurnResponseType.FINAL, resp.getType());
					assertEquals("echo:" + msg, resp.getText());
				} catch (Throwable e) {
					synchronized (failures) {
						failures.add(e);
					}
				} finally {
					latch.countDown();
				}
			});
			pool.add(t);
			t.start();
		}
		assertTrue(latch.await(30, TimeUnit.SECONDS));
		for (Thread t : pool)
			t.join();
		assertTrue("Falhas em threads: " + failures, failures.isEmpty());
		assertEquals(threads, s.store.size());
	}

	@Test
	public void backendToolWinsNameCollisionWithClientSpec() {
		FakeChatModel model = new FakeChatModel()
				.enqueueToolCalls(ToolExecutionRequest.builder().id("c1").name(BE_TOOL).arguments("{}").build())
				.enqueueText("done");
		BackendTool tool = new BackendTool(true);
		TAiToolRelayService s = service(model, 5, tool);

		// spec do cliente com o MESMO nome da tool de BE
		TAiClientToolSpec colliding = new TAiClientToolSpec();
		colliding.setName(BE_TOOL);
		colliding.setDescription("client tool colliding");
		colliding.setParametersSchemaJson("{\"type\":\"object\"}");

		TAiTurnResponse resp = s.interact(token, message(null, "oi", List.of(colliding)));
		assertEquals(TAiTurnResponseType.FINAL, resp.getType());
		// a tool de BE executou — nao virou call pendente de FE
		assertEquals(1, tool.executions.get());
		assertFalse(s.store.get(resp.getConversationId()).hasPendingTurn());

		// e a spec nao foi duplicada na uniao enviada ao LLM
		long count = model.requests.get(0).toolSpecifications().stream()
				.filter(ts -> ts.name().equals(BE_TOOL)).count();
		assertEquals(1L, count);
	}
}
