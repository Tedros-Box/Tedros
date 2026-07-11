package org.tedros.ai.toolrelay;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.tedros.ai.function.TFunction;
import org.tedros.ai.function.ToolCallResult;
import org.tedros.ai.model.TAiClientToolResult;
import org.tedros.ai.model.TAiPendingToolCall;
import org.tedros.ai.model.TAiTurnRequest;
import org.tedros.ai.model.TAiTurnRequestType;
import org.tedros.ai.model.TAiTurnResponse;
import org.tedros.ai.model.TAiTurnResponseType;
import org.tedros.ai.service.AiServiceBase;
import org.tedros.ai.service.IAiTerosService;
import org.tedros.ai.service.langchain.LangChainFunctionExecutor;
import org.tedros.util.TLoggerUtil;

import dev.langchain4j.agent.tool.ToolExecutionRequest;

/**
 * Drop-in replacement do servico de IA do chatbot no modo Tool Relay: a
 * conversa e o loop de tool calling vivem no backend; este servico apenas
 * executa as tools locais ({@link TFunction}s, via
 * {@link LangChainFunctionExecutor} reutilizado sem modificacao) e renderiza
 * o texto final.
 * <p>
 * No modo relay a API key, o modelo e o prompt vem do servidor — os
 * configurados no frontend sao ignorados.
 * <p>
 * Threading: {@code call} e bloqueante e ja e invocado fora da FX Application
 * Thread pelo chatbot atual ({@code TerosProcess}); os callbacks das
 * {@code TFunction}s executam com a mesma semantica de thread da solucao
 * atual.
 *
 * @author Davis Gordon
 */
public class ToolRelayTerosService extends AiServiceBase implements IAiTerosService {

	private static final Logger LOGGER = TLoggerUtil.getLogger(ToolRelayTerosService.class);

	private static final String AI_MODEL = "tool-relay (server-managed)";

	private static IAiTerosService instance;

	private final ToolRelayClient client;
	private final ClientToolSpecExporter specExporter;
	private LangChainFunctionExecutor functionExecutor;
	private volatile String conversationId;

	private ToolRelayTerosService() {
		this.client = new ToolRelayClient();
		this.specExporter = new ClientToolSpecExporter();
	}

	public static IAiTerosService create() {
		if (instance == null) {
			instance = new ToolRelayTerosService();
		}
		return instance;
	}

	public static IAiTerosService newInstance() {
		return new ToolRelayTerosService();
	}

	public static IAiTerosService getInstance() {
		if (instance == null)
			throw new IllegalStateException("Instance not created!");
		return instance;
	}

	@Override
	public void createFunctionExecutor(TFunction<?>... functions) {
		this.functionExecutor = new LangChainFunctionExecutor(Arrays.asList(functions));
		LOGGER.info("Registradas {} função(ões) para tool calls no modo relay.", functions.length);
	}

	@Override
	public String call(String userPrompt, String sysPrompt) {
		try {
			boolean firstTurn = conversationId == null;

			TAiTurnRequest req = new TAiTurnRequest();
			req.setType(TAiTurnRequestType.MESSAGE);
			req.setConversationId(conversationId);
			req.setUserMessage(userPrompt);
			req.setSysPrompt(sysPrompt);
			if (firstTurn)
				req.setClientToolSpecs(specExporter.export(functionExecutor));

			TAiTurnResponse resp = client.interact(req);
			this.conversationId = resp.getConversationId();

			while (resp.getType() == TAiTurnResponseType.CLIENT_TOOL_CALLS) {
				List<TAiClientToolResult> results = new ArrayList<>();
				for (TAiPendingToolCall call : resp.getPendingCalls())
					results.add(executeLocal(call));

				TAiTurnRequest toolResults = new TAiTurnRequest();
				toolResults.setType(TAiTurnRequestType.TOOL_RESULTS);
				toolResults.setConversationId(conversationId);
				toolResults.setToolResults(results);
				resp = client.interact(toolResults);
			}

			if (resp.getType() == TAiTurnResponseType.ERROR) {
				LOGGER.error("Erro no relay: {} - {}", resp.getErrorCode(), resp.getErrorMessage());
				return "Erro ao processar solicitação: "
						+ (resp.getErrorMessage() != null ? resp.getErrorMessage() : resp.getErrorCode());
			}
			return resp.getText() != null ? resp.getText() : "";
		} catch (Exception e) {
			LOGGER.error("Erro no turno do relay", e);
			return "Erro ao processar solicitação: " + e.getMessage();
		}
	}

	private TAiClientToolResult executeLocal(TAiPendingToolCall call) {
		TAiClientToolResult result = new TAiClientToolResult();
		result.setCallId(call.getCallId());
		result.setName(call.getName());

		ToolExecutionRequest request = ToolExecutionRequest.builder()
				.id(call.getCallId())
				.name(call.getName())
				.arguments(call.getArgumentsJson())
				.build();

		var resultOpt = functionExecutor != null ? functionExecutor.execute(request)
				: java.util.Optional.<ToolCallResult>empty();

		if (resultOpt.isEmpty()) {
			result.setResultJson("{\"status\":\"error\",\"error_message\":\"Function not found\"}");
			result.setRevertToModel(false);
			return result;
		}

		ToolCallResult res = resultOpt.get();
		if (res.getFilesContentInfo() != null && !res.getFilesContentInfo().isEmpty())
			LOGGER.warn("Tool {} retornou arquivos — multimodal fora do escopo da Fase 1 do relay, ignorando.",
					call.getName());

		String resultJson = "{}";
		try {
			resultJson = mapper.writeValueAsString(res.getResult());
		} catch (Exception e) {
			LOGGER.error("Erro serializando resultado da tool", e);
		}
		result.setResultJson(resultJson);
		result.setRevertToModel(res.isRevertToTheAIModelInCaseOfSuccess());
		return result;
	}

	@Override
	public void cleanMessageHistory() {
		String id = conversationId;
		this.conversationId = null;
		if (id == null)
			return;
		try {
			TAiTurnRequest close = new TAiTurnRequest();
			close.setType(TAiTurnRequestType.CLOSE);
			close.setConversationId(id);
			client.interact(close);
			LOGGER.info("Conversa {} encerrada no servidor (relay).", id);
		} catch (Exception e) {
			// a conversa sera descartada pelo TTL no servidor
			LOGGER.warn("Falha ao encerrar conversa {} no servidor: {}", id, e.getMessage());
		}
	}

	@Override
	public void setAiModel(String model) {
		LOGGER.warn("Modelo definido no servidor no modo relay — alteração ignorada.");
	}

	@Override
	public String getAiModel() {
		return AI_MODEL;
	}
}
