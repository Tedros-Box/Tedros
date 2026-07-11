package org.tedros.ai.toolrelay;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.tedros.ai.model.TAiClientToolResult;
import org.tedros.ai.model.TAiPendingToolCall;
import org.tedros.ai.model.TAiTurnResponse;
import org.tedros.ai.model.TAiTurnResponseType;
import org.tedros.ai.observability.TAiMetrics;
import org.tedros.ai.toolrelay.function.TAiToolContext;
import org.tedros.ai.toolrelay.function.TServerAiFunction;
import org.tedros.ai.toolrelay.function.TServerFunctionCatalog;
import org.tedros.server.util.TLoggerUtil;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.json.JsonSanitizer;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;
import io.micrometer.core.instrument.Timer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Loop de tool calling do relay — adaptacao server-side do
 * {@code processConversationLoop} do {@code LangChainOpenAITerosService} (FE):
 * <ul>
 *   <li>as specs enviadas ao LLM sao a uniao catalogo de BE + specs do cliente
 *       (colisao de nome: BE vence, com warning);</li>
 *   <li>tool de backend executa inline; tool de frontend suspende o turno e
 *       devolve {@code CLIENT_TOOL_CALLS} — nenhuma thread fica bloqueada
 *       esperando o cliente;</li>
 *   <li>profundidade maxima de {@value #MAX_RECURSION_DEPTH} iteracoes POR
 *       TURNO, sobrevivendo a suspensoes (contador no estado da conversa);</li>
 *   <li>multimodal (arquivos em resultados) esta fora do escopo da Fase 1.</li>
 * </ul>
 * O chamador (service) DEVE deter o lock da conversa.
 *
 * @author Davis Gordon
 */
@ApplicationScoped
public class TAiRelayLoop {

	private static final TLoggerUtil LOGGER = TLoggerUtil.create(TAiRelayLoop.class);

	/** Mesmo valor do FE ({@code LangChainOpenAITerosService.MAX_RECURSION_DEPTH}). */
	public static final int MAX_RECURSION_DEPTH = 15;

	public static final String ERROR_MAX_DEPTH = "MAX_DEPTH";
	public static final String ERROR_LLM = "LLM_ERROR";

	private static final String FUNCTION_NOT_FOUND = "Function not found";
	private static final String CANCELLED_RESULT_JSON = "{\"status\":\"cancelled\"}";

	// Instancia unica e thread-safe — nao criar um mapper por serializacao
	private static final ObjectMapper MAPPER = new ObjectMapper();

	@Inject
	TAiMetrics metrics;

	/**
	 * Roda o loop ate resposta final, suspensao por tools de frontend, limite
	 * de profundidade ou erro do LLM. O {@code ctx} carrega o token do request
	 * CORRENTE — repassado as tools de backend a cada turno. {@code provider} e
	 * {@code model} sao usados apenas como labels de baixa cardinalidade nas
	 * metricas de LLM/tokens.
	 */
	public TAiTurnResponse run(TAiConversation conv, ChatModel model, TServerFunctionCatalog catalog,
			TAiToolContext ctx, String provider, String modelName) {

		List<ToolSpecification> tools = unionSpecifications(conv, catalog);

		while (true) {

			if (conv.getTurnDepth() >= MAX_RECURSION_DEPTH) {
				LOGGER.warn("AI relay recursion limit reached ({}) on conversation {}",
						MAX_RECURSION_DEPTH, conv.getId());
				return error(ERROR_MAX_DEPTH, "Tool call limit reached for this turn.");
			}

			ChatResponse response;
			Timer.Sample llmSample = metrics != null ? metrics.startLlm() : null;
			try {
				response = model.chat(ChatRequest.builder()
						.messages(conv.getMemory().messages())
						.toolSpecifications(tools)
						.build());
			} catch (Exception e) {
				if (metrics != null)
					metrics.stopLlm(llmSample, provider, modelName, "error");
				LOGGER.error("Error calling the LLM on conversation " + conv.getId(), e);
				return error(ERROR_LLM, e.getMessage());
			}
			if (metrics != null) {
				metrics.stopLlm(llmSample, provider, modelName, "success");
				recordTokens(provider, modelName, response.tokenUsage());
			}
			conv.incrementTurnDepth();
			conv.accumulateTokenUsage(response.tokenUsage());

			AiMessage aiMessage = response.aiMessage();
			// FIX (mesmo do FE): a API OpenAI rejeita content null no historico
			if (aiMessage.text() == null) {
				aiMessage = aiMessage.hasToolExecutionRequests()
						? new AiMessage("", aiMessage.toolExecutionRequests())
						: new AiMessage("");
			}
			conv.getMemory().add(aiMessage);
			conv.setLastAiText(aiMessage.text());

			if (!aiMessage.hasToolExecutionRequests()) {
				TAiTurnResponse resp = new TAiTurnResponse();
				resp.setType(TAiTurnResponseType.FINAL);
				resp.setText(aiMessage.text());
				return resp;
			}

			List<ToolExecutionRequest> requests = aiMessage.toolExecutionRequests();
			Map<String, ToolExecutionResultMessage> backendResults = new LinkedHashMap<>();
			List<ToolExecutionRequest> frontendCalls = new ArrayList<>();
			boolean backendRevert = false;

			for (ToolExecutionRequest req : requests) {
				if (catalog.contains(req.name())) {
					// colisao de nome com spec do cliente: BE vence
					if (conv.hasClientTool(req.name()))
						LOGGER.warn("Tool name '{}' exists on both backend and client — backend wins",
								req.name());
					Optional<TServerAiFunction> fn = catalog.byName(req.name());
					backendResults.put(req.id(), executeBackend(fn.get(), req, ctx));
					if (fn.get().revertToModel())
						backendRevert = true;
				} else if (conv.hasClientTool(req.name())) {
					recordTool(ctx, req.name(), "client", -1);
					frontendCalls.add(req);
				} else {
					// mesmo comportamento do FE: result "Function not found", sem reloop
					recordTool(ctx, req.name(), "not_found", -1);
					backendResults.put(req.id(), ToolExecutionResultMessage.from(req, FUNCTION_NOT_FOUND));
				}
			}

			if (frontendCalls.isEmpty()) {
				// so tools de BE: anexa os results na ordem original e decide reloop
				requests.forEach(req -> conv.getMemory().add(backendResults.get(req.id())));
				if (backendRevert)
					continue;
				TAiTurnResponse resp = new TAiTurnResponse();
				resp.setType(TAiTurnResponseType.FINAL);
				resp.setText(conv.getLastAiText());
				return resp;
			}

			// ha tools de frontend: suspende o turno (o estado fica na conversa,
			// nenhuma thread bloqueada) e devolve as calls pendentes ao cliente
			Set<String> pendingIds = new LinkedHashSet<>();
			frontendCalls.forEach(req -> pendingIds.add(req.id()));
			conv.setPendingTurn(new TAiConversation.PendingTurn(requests, backendResults,
					backendRevert, pendingIds));

			TAiTurnResponse resp = new TAiTurnResponse();
			resp.setType(TAiTurnResponseType.CLIENT_TOOL_CALLS);
			resp.setText(aiMessage.text());
			List<TAiPendingToolCall> calls = new ArrayList<>();
			for (ToolExecutionRequest req : frontendCalls) {
				TAiPendingToolCall call = new TAiPendingToolCall();
				call.setCallId(req.id());
				call.setName(req.name());
				call.setArgumentsJson(req.arguments());
				calls.add(call);
			}
			resp.setPendingCalls(calls);
			return resp;
		}
	}

	/**
	 * Retoma um turno suspenso com os resultados das tools de frontend: monta
	 * as {@code ToolExecutionResultMessage}s de TODAS as calls na ordem dos
	 * {@code ToolExecutionRequest}s originais e, se algum resultado pedir
	 * retorno ao modelo, continua o loop; senao encerra com o texto da ultima
	 * {@code AiMessage} sem nova chamada ao LLM (os results entram na memoria
	 * mesmo assim — a API exige um result por tool call).
	 * <p>
	 * O chamador ja validou que os callIds conferem com os pendentes.
	 */
	public TAiTurnResponse resume(TAiConversation conv, ChatModel model, TServerFunctionCatalog catalog,
			List<TAiClientToolResult> results, TAiToolContext ctx, String provider, String modelName) {

		TAiConversation.PendingTurn pending = conv.getPendingTurn();

		Map<String, TAiClientToolResult> byCallId = new LinkedHashMap<>();
		results.forEach(r -> byCallId.put(r.getCallId(), r));

		boolean revert = pending.isBackendRevert();
		for (ToolExecutionRequest req : pending.getRequests()) {
			ToolExecutionResultMessage msg = pending.getBackendResults().get(req.id());
			if (msg == null) {
				TAiClientToolResult result = byCallId.get(req.id());
				String json = result.getResultJson() != null ? result.getResultJson() : "{}";
				msg = ToolExecutionResultMessage.from(req, json);
				if (result.isRevertToModel())
					revert = true;
			}
			conv.getMemory().add(msg);
		}
		conv.setPendingTurn(null);

		if (revert)
			return run(conv, model, catalog, ctx, provider, modelName);

		TAiTurnResponse resp = new TAiTurnResponse();
		resp.setType(TAiTurnResponseType.FINAL);
		resp.setText(conv.getLastAiText());
		return resp;
	}

	/**
	 * Descarta um turno pendente expirado, anexando os results de BE ja
	 * executados e results sinteticos {@code {"status":"cancelled"}} as calls
	 * orfas — mantem a memoria valida para o proximo turno.
	 */
	public void sanitizeExpiredPendingTurn(TAiConversation conv) {
		TAiConversation.PendingTurn pending = conv.getPendingTurn();
		if (pending == null)
			return;
		LOGGER.warn("Discarding expired pending turn on conversation {}", conv.getId());
		for (ToolExecutionRequest req : pending.getRequests()) {
			ToolExecutionResultMessage msg = pending.getBackendResults().get(req.id());
			conv.getMemory().add(msg != null ? msg
					: ToolExecutionResultMessage.from(req, CANCELLED_RESULT_JSON));
		}
		conv.setPendingTurn(null);
	}

	private List<ToolSpecification> unionSpecifications(TAiConversation conv, TServerFunctionCatalog catalog) {
		List<ToolSpecification> tools = new ArrayList<>(catalog.toolSpecifications());
		for (ToolSpecification spec : conv.getClientToolSpecifications()) {
			if (catalog.contains(spec.name())) {
				LOGGER.warn("Client tool '{}' collides with a backend tool — backend wins", spec.name());
				continue;
			}
			tools.add(spec);
		}
		return tools;
	}

	private ToolExecutionResultMessage executeBackend(TServerAiFunction fn, ToolExecutionRequest req,
			TAiToolContext ctx) {
		LOGGER.info("AI relay backend tool call: {} args: {}", req.name(), req.arguments());
		long start = System.currentTimeMillis();
		try {
			String args = (req.arguments() == null || req.arguments().isBlank()) ? "{}"
					: JsonSanitizer.sanitize(req.arguments());
			Object arg = MAPPER.readValue(args, fn.getModel());
			Object result = fn.execute(arg, ctx);
			ToolExecutionResultMessage msg = ToolExecutionResultMessage.from(req, MAPPER.writeValueAsString(result));
			recordTool(ctx, req.name(), "success", System.currentTimeMillis() - start);
			return msg;
		} catch (Exception e) {
			recordTool(ctx, req.name(), "error", System.currentTimeMillis() - start);
			LOGGER.error("Error executing backend tool " + req.name(), e);
			return ToolExecutionResultMessage.from(req,
					"{\"status\":\"error\",\"error_message\":" + quote(e.getMessage()) + "}");
		}
	}

	/** Credita os tokens de UMA resposta do LLM (por iteracao do loop). */
	private void recordTokens(String provider, String modelName, TokenUsage usage) {
		if (metrics == null || usage == null)
			return;
		Long input = usage.inputTokenCount() != null ? usage.inputTokenCount().longValue() : null;
		Long output = usage.outputTokenCount() != null ? usage.outputTokenCount().longValue() : null;
		metrics.recordTokens(provider, modelName, input, output);
	}

	/** Uma chamada de tool; nunca lanca (observabilidade nao pode quebrar o loop). */
	private void recordTool(TAiToolContext ctx, String tool, String outcome, long millis) {
		if (metrics != null)
			metrics.recordTool(tool, outcome, millis);
		if (ctx != null)
			ctx.addToolCall(tool, outcome, millis);
	}

	private static String quote(String value) {
		try {
			return MAPPER.writeValueAsString(value != null ? value : "unknown error");
		} catch (Exception e) {
			return "\"unknown error\"";
		}
	}

	private TAiTurnResponse error(String code, String message) {
		TAiTurnResponse resp = new TAiTurnResponse();
		resp.setType(TAiTurnResponseType.ERROR);
		resp.setErrorCode(code);
		resp.setErrorMessage(message);
		return resp;
	}
}
