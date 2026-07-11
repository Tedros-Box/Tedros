package org.tedros.ai.ejb.service;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.tedros.ai.model.TAiClientToolResult;
import org.tedros.ai.model.TAiTokenUsage;
import org.tedros.ai.model.TAiTurnRequest;
import org.tedros.ai.model.TAiTurnRequestType;
import org.tedros.ai.model.TAiTurnResponse;
import org.tedros.ai.model.TAiTurnResponseType;
import org.tedros.ai.observability.TAiMetrics;
import org.tedros.ai.observability.TAiUsageEventSink;
import org.tedros.ai.observability.entity.TAiUsageEvent;
import org.tedros.ai.toolrelay.ITAiConversationStore;
import org.tedros.ai.toolrelay.TAiConversation;
import org.tedros.ai.toolrelay.TAiRelayConfig;
import org.tedros.ai.toolrelay.TAiRelayConfigSnapshot;
import org.tedros.ai.toolrelay.TAiRelayLoop;
import org.tedros.ai.toolrelay.TRelayModelAdapter;
import org.tedros.ai.toolrelay.function.TAiToolContext;
import org.tedros.ai.toolrelay.function.TServerFunctionCatalog;
import org.tedros.core.security.model.TUser;
import org.tedros.server.ejb.controller.ITSecurityController;
import org.tedros.server.entity.ITUser;
import org.tedros.server.security.TAccessToken;
import org.tedros.server.util.TLoggerUtil;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.output.TokenUsage;
import jakarta.ejb.EJB;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;

/**
 * Orquestra o protocolo do Tool Relay (maquina de estados do turno):
 * <ol>
 *   <li>{@code MESSAGE} sem conversationId cria a conversa, registra as specs
 *       do cliente e roda o loop;</li>
 *   <li>tool de backend executa inline; tool de frontend suspende o turno e
 *       responde {@code CLIENT_TOOL_CALLS};</li>
 *   <li>{@code TOOL_RESULTS} valida os callIds pendentes e retoma o loop (ou
 *       encerra sem nova chamada ao LLM se nenhum resultado pedir retorno);</li>
 *   <li>{@code CLOSE} descarta a conversa.</li>
 * </ol>
 * Sem transacao JTA durante as chamadas longas ao LLM; lock por conversa com
 * {@code tryLock} — turno concorrente na MESMA conversa responde
 * {@code TURN_IN_PROGRESS} imediatamente.
 *
 * @author Davis Gordon
 */
@LocalBean
@Stateless(name = "TAiToolRelayService")
@TransactionAttribute(value = TransactionAttributeType.NOT_SUPPORTED)
public class TAiToolRelayService {

	private static final TLoggerUtil LOGGER = TLoggerUtil.create(TAiToolRelayService.class);

	public static final String ERROR_AI_DISABLED = "AI_DISABLED";
	public static final String ERROR_TURN_IN_PROGRESS = "TURN_IN_PROGRESS";
	public static final String ERROR_CONVERSATION_NOT_FOUND = "CONVERSATION_NOT_FOUND";
	public static final String ERROR_NO_PENDING_TURN = "NO_PENDING_TURN";
	public static final String ERROR_INVALID_TOOL_RESULTS = "INVALID_TOOL_RESULTS";
	public static final String ERROR_INVALID_REQUEST = "INVALID_REQUEST";
	public static final String ERROR_ACCESS_DENIED = "ACCESS_DENIED";
	public static final String ERROR_INTERNAL = "INTERNAL_ERROR";

	// Campos package-private para permitir injecao manual nos testes de unidade
	@EJB
	ITSecurityController securityController;

	@Inject
	TAiRelayConfig config;

	@Inject
	TRelayModelAdapter modelAdapter;

	@Inject
	TServerFunctionCatalog catalog;

	@Inject
	ITAiConversationStore store;

	@Inject
	TAiRelayLoop loop;

	@Inject
	TAiMetrics metrics;

	@EJB
	TAiUsageEventSink usageSink;

	// serializa a lista de tool calls do turno para o evento de consumo por usuario
	private static final ObjectMapper USAGE_MAPPER = new ObjectMapper();

	public TAiTurnResponse interact(TAccessToken token, TAiTurnRequest request) {
		long start = System.currentTimeMillis();
		TAiTurnResponse resp = null;
		String conversationId = request != null ? request.getConversationId() : null;
		Long userId = null;
		String userName = null;
		TAiTokenUsage usage = null;
		TAiRelayConfigSnapshot cfg = null;
		TAiToolContext ctx = null;
		try {
			if (request == null || request.getType() == null)
				return resp = error(null, ERROR_INVALID_REQUEST, "Request or request type is null");

			if (request.getType() == TAiTurnRequestType.CLOSE) {
				store.remove(conversationId);
				resp = new TAiTurnResponse();
				resp.setType(TAiTurnResponseType.FINAL);
				resp.setConversationId(conversationId);
				return resp;
			}

			cfg = config.snapshot();
			if (!cfg.isAiEnabled())
				return resp = error(conversationId, ERROR_AI_DISABLED, "Artificial intelligence is disabled");

			TUser user = resolveUser(token);
			if (user == null)
				return resp = error(conversationId, ERROR_ACCESS_DENIED, "User not found for the given token");
			userId = user.getId();
			userName = user.getName();

			// token do request CORRENTE — tokens podem expirar entre turnos
			ctx = new TAiToolContext(user, token);

			resp = switch (request.getType()) {
			case MESSAGE -> onMessage(request, ctx, cfg);
			case TOOL_RESULTS -> onToolResults(request, ctx, cfg);
			default -> error(conversationId, ERROR_INVALID_REQUEST, "Unsupported request type");
			};
			conversationId = resp.getConversationId();
			usage = resp.getTokenUsage();
			return resp;
		} catch (Exception e) {
			LOGGER.error("Unexpected error on AI relay interact", e);
			return resp = error(conversationId, ERROR_INTERNAL, e.getMessage());
		} finally {
			long elapsedMs = System.currentTimeMillis() - start;
			LOGGER.info("AI relay turn | user {} | conversation {} | tokens in {} out {} total {} | {} ms",
					userId, conversationId,
					usage != null ? usage.getInput() : null,
					usage != null ? usage.getOutput() : null,
					usage != null ? usage.getTotal() : null,
					elapsedMs);
			recordTurnMetrics(request, resp, conversationId, elapsedMs);
			emitUsageEvent(request, resp, conversationId, userId, userName, cfg, usage, ctx, elapsedMs);
		}
	}

	/**
	 * Emite (assincrono, fire-and-forget) o evento de consumo por usuario deste
	 * turno. Nunca lanca: uma falha de telemetria nao pode afetar a resposta ao
	 * cliente. {@code CLOSE} e requests sem usuario resolvido nao geram evento.
	 */
	private void emitUsageEvent(TAiTurnRequest request, TAiTurnResponse resp, String conversationId,
			Long userId, String userName, TAiRelayConfigSnapshot cfg, TAiTokenUsage usage,
			TAiToolContext ctx, long elapsedMs) {
		if (usageSink == null)
			return;
		try {
			TAiTurnRequestType reqType = request != null ? request.getType() : null;
			if (reqType == null || reqType == TAiTurnRequestType.CLOSE || userId == null)
				return;

			TAiUsageEvent ev = new TAiUsageEvent();
			ev.setTs(new Date());
			ev.setUserId(userId);
			ev.setUserName(userName);
			ev.setConversationId(conversationId);
			ev.setTurnType(reqType.name());
			if (cfg != null) {
				ev.setProvider(providerLabel(cfg));
				ev.setModel(cfg.getModel());
			}
			if (usage != null) {
				ev.setInputTokens(usage.getInput());
				ev.setOutputTokens(usage.getOutput());
				ev.setTotalTokens(usage.getTotal());
			}
			ev.setTurnMs((int) Math.min(Integer.MAX_VALUE, elapsedMs));

			TAiConversation conv = conversationId != null ? store.get(conversationId) : null;
			if (conv != null)
				ev.setTurnDepth(conv.getTurnDepth());

			TAiTurnResponseType respType = resp != null ? resp.getType() : TAiTurnResponseType.ERROR;
			ev.setOutcome(respType != null ? respType.name() : TAiTurnResponseType.ERROR.name());
			if (respType == TAiTurnResponseType.ERROR)
				ev.setErrorCode(resp != null ? resp.getErrorCode() : ERROR_INTERNAL);

			ev.setToolCalls(toolCallsJson(ctx));
			usageSink.emit(ev);
		} catch (Exception e) {
			LOGGER.warn("Failed to build/emit AI usage event: {}", e.getMessage());
		}
	}

	/** JSON {@code [{name,outcome,ms}]} das tools do turno, ou null se vazio/grande demais. */
	private String toolCallsJson(TAiToolContext ctx) {
		if (ctx == null || ctx.getToolCalls().isEmpty())
			return null;
		try {
			String json = USAGE_MAPPER.writeValueAsString(ctx.getToolCalls());
			// nunca truncar (invalidaria o JSON): acima do limite da coluna, descarta
			return json.length() <= TAiUsageEvent.TOOL_CALLS_MAX ? json : null;
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Telemetria numerica do turno (choke point). Nunca lanca: observabilidade
	 * jamais pode quebrar o turno. {@code CLOSE} nao e um turno de IA e nao e
	 * instrumentado.
	 */
	private void recordTurnMetrics(TAiTurnRequest request, TAiTurnResponse resp, String conversationId,
			long elapsedMs) {
		if (metrics == null)
			return;
		try {
			TAiTurnRequestType reqType = request != null ? request.getType() : null;
			if (reqType == TAiTurnRequestType.CLOSE)
				return;
			TAiTurnResponseType respType = resp != null ? resp.getType() : TAiTurnResponseType.ERROR;
			String type = reqType != null ? reqType.name() : "UNKNOWN";
			String outcome = respType != null ? respType.name() : TAiTurnResponseType.ERROR.name();

			int depth = 0;
			TAiConversation conv = conversationId != null ? store.get(conversationId) : null;
			if (conv != null)
				depth = conv.getTurnDepth();

			metrics.recordTurn(type, outcome, elapsedMs, depth);
			if (respType == TAiTurnResponseType.ERROR)
				metrics.recordError(resp != null ? resp.getErrorCode() : ERROR_INTERNAL);
		} catch (Exception e) {
			LOGGER.warn("Failed to record AI relay turn metrics: {}", e.getMessage());
		}
	}

	private TAiTurnResponse onMessage(TAiTurnRequest request, TAiToolContext ctx, TAiRelayConfigSnapshot cfg) {

		TUser user = ctx.getUser();
		if (request.getUserMessage() == null || request.getUserMessage().isBlank())
			return error(request.getConversationId(), ERROR_INVALID_REQUEST, "userMessage is required");

		TAiConversation conv;
		boolean firstTurn = request.getConversationId() == null;
		if (firstTurn) {
			conv = store.create(UUID.randomUUID().toString(), user, cfg);
			conv.registerClientSpecs(request.getClientToolSpecs());
		} else {
			conv = requireConversation(request.getConversationId(), user);
			if (conv == null)
				return error(request.getConversationId(), ERROR_CONVERSATION_NOT_FOUND,
						"Conversation not found");
		}

		if (!conv.getLock().tryLock())
			return error(conv.getId(), ERROR_TURN_IN_PROGRESS, "A turn is already running");
		try {
			conv.touch();
			if (conv.hasPendingTurn()) {
				if (conv.getPendingTurn().isExpired(cfg.getPendingTurnTtlMin()))
					loop.sanitizeExpiredPendingTurn(conv);
				else
					return error(conv.getId(), ERROR_TURN_IN_PROGRESS,
							"A turn is pending client tool results");
			}

			if (conv.getMemory().messages().isEmpty())
				conv.getMemory().add(SystemMessage.from(systemPrompt(user, cfg, request.getSysPrompt())));

			conv.resetTurn();
			conv.getMemory().add(UserMessage.from(request.getUserMessage()));

			ChatModel model = modelAdapter.modelFor(cfg);
			TAiTurnResponse resp = loop.run(conv, model, catalog, ctx, providerLabel(cfg), cfg.getModel());
			return finish(resp, conv);
		} finally {
			conv.getLock().unlock();
		}
	}

	private TAiTurnResponse onToolResults(TAiTurnRequest request, TAiToolContext ctx, TAiRelayConfigSnapshot cfg) {

		TAiConversation conv = requireConversation(request.getConversationId(), ctx.getUser());
		if (conv == null)
			return error(request.getConversationId(), ERROR_CONVERSATION_NOT_FOUND,
					"Conversation not found");

		if (!conv.getLock().tryLock())
			return error(conv.getId(), ERROR_TURN_IN_PROGRESS, "A turn is already running");
		try {
			conv.touch();
			if (!conv.hasPendingTurn())
				return error(conv.getId(), ERROR_NO_PENDING_TURN,
						"There is no pending turn on this conversation");
			if (conv.getPendingTurn().isExpired(cfg.getPendingTurnTtlMin())) {
				loop.sanitizeExpiredPendingTurn(conv);
				return error(conv.getId(), ERROR_NO_PENDING_TURN, "The pending turn has expired");
			}
			if (!callIdsMatch(request, conv))
				return error(conv.getId(), ERROR_INVALID_TOOL_RESULTS,
						"Tool results do not match the pending tool calls");

			ChatModel model = modelAdapter.modelFor(cfg);
			TAiTurnResponse resp = loop.resume(conv, model, catalog, request.getToolResults(), ctx,
					providerLabel(cfg), cfg.getModel());
			return finish(resp, conv);
		} finally {
			conv.getLock().unlock();
		}
	}

	private boolean callIdsMatch(TAiTurnRequest request, TAiConversation conv) {
		if (request.getToolResults() == null)
			return false;
		Set<String> received = new HashSet<>();
		for (TAiClientToolResult r : request.getToolResults()) {
			if (r.getCallId() == null)
				return false;
			received.add(r.getCallId());
		}
		return received.equals(conv.getPendingTurn().getPendingCallIds());
	}

	private TAiConversation requireConversation(String conversationId, TUser user) {
		TAiConversation conv = store.get(conversationId);
		// conversa de outro usuario e tratada como inexistente
		if (conv == null || conv.getUserId() == null || !conv.getUserId().equals(user.getId()))
			return null;
		return conv;
	}

	private static String providerLabel(TAiRelayConfigSnapshot cfg) {
		return cfg.getProvider() != null ? cfg.getProvider().name() : null;
	}

	private TUser resolveUser(TAccessToken token) {
		// nome/identidade sempre resolvidos do token via servico de seguranca —
		// nunca confiar em dados de identificacao vindos do cliente
		ITUser user = securityController.getUser(token);
		return user instanceof TUser tu ? tu : null;
	}

	private String systemPrompt(TUser user, TAiRelayConfigSnapshot cfg, String requestSysPrompt) {
		String prompt = """
				### System information:
				Date: %s
				User Name: %s
				""".formatted(
				ZonedDateTime.now().format(DateTimeFormatter
						.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.SHORT)
						.withLocale(Locale.getDefault())),
				user.getName());
		prompt += cfg.getPrompt() != null ? cfg.getPrompt() : "";
		if (requestSysPrompt != null && !requestSysPrompt.isBlank())
			prompt += "\n" + requestSysPrompt;
		return prompt;
	}

	private TAiTurnResponse finish(TAiTurnResponse resp, TAiConversation conv) {
		resp.setConversationId(conv.getId());
		resp.setTokenUsage(toTokenUsage(conv.getTurnTokenUsage()));
		conv.touch();
		return resp;
	}

	private TAiTokenUsage toTokenUsage(TokenUsage usage) {
		if (usage == null)
			return null;
		TAiTokenUsage t = new TAiTokenUsage();
		t.setInput(usage.inputTokenCount() != null ? usage.inputTokenCount().longValue() : null);
		t.setOutput(usage.outputTokenCount() != null ? usage.outputTokenCount().longValue() : null);
		t.setTotal(usage.totalTokenCount() != null ? usage.totalTokenCount().longValue() : null);
		return t;
	}

	private TAiTurnResponse error(String conversationId, String code, String message) {
		TAiTurnResponse resp = new TAiTurnResponse();
		resp.setType(TAiTurnResponseType.ERROR);
		resp.setConversationId(conversationId);
		resp.setErrorCode(code);
		resp.setErrorMessage(message);
		return resp;
	}
}
