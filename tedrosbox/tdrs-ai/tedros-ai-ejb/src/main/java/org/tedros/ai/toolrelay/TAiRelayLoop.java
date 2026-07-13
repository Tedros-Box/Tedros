package org.tedros.ai.toolrelay;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.tedros.ai.model.TAiClientToolResult;
import org.tedros.ai.model.TAiPendingToolCall;
import org.tedros.ai.model.TAiTurnResponse;
import org.tedros.ai.model.TAiTurnResponseType;
import org.tedros.ai.observability.TAiMetrics;
import org.tedros.ai.observability.pricing.TAiCallUsage;
import org.tedros.ai.observability.pricing.TAiPriceBook;
import org.tedros.ai.observability.pricing.TAiUsageExtractor;
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

	@Inject
	TAiUsageExtractor extractor;

	@Inject
	TAiPriceBook priceBook;

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
		// Gemini exige preservar a AiMessage original quando ha tool calls: uma nova
		// instancia descarta o attributes com a thoughtSignature (ver bloco abaixo).
		boolean isGemini = "GEMINI".equalsIgnoreCase(provider);

		while (true) {

			if (conv.getTurnDepth() >= MAX_RECURSION_DEPTH) {
				LOGGER.warn("AI relay recursion limit reached ({}) on conversation {}",
						MAX_RECURSION_DEPTH, conv.getId());
				return error(ERROR_MAX_DEPTH, "Tool call limit reached for this turn.");
			}

			ChatResponse response;
			Timer.Sample llmSample = metrics != null ? metrics.startLlm() : null;
			long llmStart = System.currentTimeMillis();
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
			long llmMs = System.currentTimeMillis() - llmStart;
			if (metrics != null)
				metrics.stopLlm(llmSample, provider, modelName, "success");

			// uso/custo por chamada (grao de billing): credita metricas e acumula
			// na conversa (fonte do ledger na Parte 4). callIndex = profundidade
			// ANTES do incremento (0-based).
			meterCall(conv, provider, modelName, response.tokenUsage(), llmMs);

			conv.incrementTurnDepth();
			conv.accumulateTokenUsage(response.tokenUsage());

			AiMessage aiMessage = response.aiMessage();
			// Normalizacao de content null no historico — diverge por provider:
			//  - OpenAI/GROK rejeitam content null mesmo com tool calls, entao
			//    reconstroi com texto vazio (igual ao LangChainOpenAITerosService do FE);
			//  - Gemini NAO pode ser reconstruido quando ha tool calls: uma nova
			//    AiMessage descarta o attributes onde fica a thoughtSignature do Gemini
			//    2.5+, e sem a assinatura o proximo turno quebra com "function call turn
			//    must come immediately after a user turn or after a function response
			//    turn". Preserva o objeto original (igual ao LangChainGeminiTerosService
			//    do FE). O reenvio da assinatura depende de sendThinking(true) no adapter.
			if (aiMessage.text() == null) {
				if (!aiMessage.hasToolExecutionRequests())
					aiMessage = new AiMessage("");
				else if (!isGemini)
					aiMessage = new AiMessage("", aiMessage.toolExecutionRequests());
			}
			conv.getMemory().add(aiMessage);
			// texto do turno nunca nulo: a AiMessage do Gemini preservada pode ter text null
			conv.setLastAiText(aiMessage.text() != null ? aiMessage.text() : "");

			if (!aiMessage.hasToolExecutionRequests()) {
				TAiTurnResponse resp = new TAiTurnResponse();
				resp.setType(TAiTurnResponseType.FINAL);
				resp.setText(aiMessage.text());
				return resp;
			}

			// Indexa as calls pelo callId do PROTOCOLO do relay: Gemini nao gera id
			// nas function calls (req.id() null), o que colidia nos mapas e quebrava
			// a validacao de TOOL_RESULTS no cliente. O callId sintetico circula so
			// entre relay e cliente — a memoria guarda a AiMessage e os results com
			// os requests ORIGINAIS (functionResponse volta ao Gemini sem id, como
			// no FE). Para OpenAI/GROK o callId e o proprio id do provider.
			Map<String, ToolExecutionRequest> callsById = new LinkedHashMap<>();
			for (ToolExecutionRequest req : aiMessage.toolExecutionRequests())
				callsById.put(protocolCallId(req), req);

			Map<String, ToolExecutionResultMessage> backendResults = new LinkedHashMap<>();
			Map<String, ToolExecutionRequest> frontendCalls = new LinkedHashMap<>();
			boolean backendRevert = false;

			for (Map.Entry<String, ToolExecutionRequest> entry : callsById.entrySet()) {
				String callId = entry.getKey();
				ToolExecutionRequest req = entry.getValue();
				if (catalog.contains(req.name())) {
					// colisao de nome com spec do cliente: BE vence
					if (conv.hasClientTool(req.name()))
						LOGGER.warn("Tool name '{}' exists on both backend and client — backend wins",
								req.name());
					Optional<TServerAiFunction> fn = catalog.byName(req.name());
					backendResults.put(callId, executeBackend(fn.get(), req, ctx));
					if (fn.get().revertToModel())
						backendRevert = true;
				} else if (conv.hasClientTool(req.name())) {
					recordTool(ctx, req.name(), "client", -1);
					frontendCalls.put(callId, req);
				} else {
					// mesmo comportamento do FE: result "Function not found", sem reloop
					recordTool(ctx, req.name(), "not_found", -1);
					backendResults.put(callId, ToolExecutionResultMessage.from(req, FUNCTION_NOT_FOUND));
				}
			}

			if (frontendCalls.isEmpty()) {
				// so tools de BE: anexa os results na ordem original e decide reloop
				callsById.keySet().forEach(callId -> conv.getMemory().add(backendResults.get(callId)));
				if (backendRevert)
					continue;
				TAiTurnResponse resp = new TAiTurnResponse();
				resp.setType(TAiTurnResponseType.FINAL);
				resp.setText(conv.getLastAiText());
				return resp;
			}

			// ha tools de frontend: suspende o turno (o estado fica na conversa,
			// nenhuma thread bloqueada) e devolve as calls pendentes ao cliente
			Set<String> pendingIds = new LinkedHashSet<>(frontendCalls.keySet());
			conv.setPendingTurn(new TAiConversation.PendingTurn(callsById, backendResults,
					backendRevert, pendingIds));

			TAiTurnResponse resp = new TAiTurnResponse();
			resp.setType(TAiTurnResponseType.CLIENT_TOOL_CALLS);
			// lastAiText ja normalizado (nunca nulo) a partir desta mesma AiMessage
			resp.setText(conv.getLastAiText());
			List<TAiPendingToolCall> calls = new ArrayList<>();
			for (Map.Entry<String, ToolExecutionRequest> entry : frontendCalls.entrySet()) {
				TAiPendingToolCall call = new TAiPendingToolCall();
				call.setCallId(entry.getKey());
				call.setName(entry.getValue().name());
				call.setArgumentsJson(entry.getValue().arguments());
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
		for (Map.Entry<String, ToolExecutionRequest> entry : pending.getRequestsByCallId().entrySet()) {
			ToolExecutionResultMessage msg = pending.getBackendResults().get(entry.getKey());
			if (msg == null) {
				TAiClientToolResult result = byCallId.get(entry.getKey());
				String json = result.getResultJson() != null ? result.getResultJson() : "{}";
				// result montado do request ORIGINAL: mantem o id do provider (null
				// no Gemini), nunca o callId sintetico do protocolo
				msg = ToolExecutionResultMessage.from(entry.getValue(), json);
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
		for (Map.Entry<String, ToolExecutionRequest> entry : pending.getRequestsByCallId().entrySet()) {
			ToolExecutionResultMessage msg = pending.getBackendResults().get(entry.getKey());
			conv.getMemory().add(msg != null ? msg
					: ToolExecutionResultMessage.from(entry.getValue(), CANCELLED_RESULT_JSON));
		}
		conv.setPendingTurn(null);
	}

	/**
	 * Identidade da tool call no protocolo do relay: o id do provider quando
	 * existe (OpenAI/GROK), ou um id sintetico quando o provider nao gera id
	 * (Gemini). Circula apenas nos mapas internos e no round-trip com o cliente
	 * — jamais entra na memoria da conversa.
	 */
	private static String protocolCallId(ToolExecutionRequest req) {
		return (req.id() == null || req.id().isBlank())
				? "relay_" + UUID.randomUUID()
				: req.id();
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

	/**
	 * Extrai o uso faturavel de UMA resposta, precifica, credita as metricas de
	 * tokens/custo e acumula a chamada no turno (fonte do ledger). Nunca lanca:
	 * telemetria/custo jamais pode quebrar o turno. {@code priceBook.cost} tambem
	 * grava o {@code priceVersion} no {@code call}.
	 */
	private void meterCall(TAiConversation conv, String provider, String modelName, TokenUsage usage, long llmMs) {
		if (extractor == null)
			return;
		try {
			TAiCallUsage call = extractor.extract(conv.getTurnDepth(), provider, modelName, usage, llmMs);
			if (priceBook != null)
				call.setCostUsd(priceBook.cost(call));
			if (metrics != null) {
				metrics.recordTokens(call);
				metrics.recordCost(call);
			}
			conv.addCallUsage(call);
		} catch (Exception e) {
			LOGGER.warn("Failed to meter LLM call usage on conversation {}: {}", conv.getId(), e.getMessage());
		}
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
