package org.tedros.ai.ejb.service;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
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
import org.tedros.ai.toolrelay.ITAiConversationStore;
import org.tedros.ai.toolrelay.TAiConversation;
import org.tedros.ai.toolrelay.TAiRelayConfig;
import org.tedros.ai.toolrelay.TAiRelayConfigSnapshot;
import org.tedros.ai.toolrelay.TAiRelayLoop;
import org.tedros.ai.toolrelay.TRelayModelAdapter;
import org.tedros.ai.toolrelay.function.TServerFunctionCatalog;
import org.tedros.core.security.model.TUser;
import org.tedros.server.ejb.controller.ITSecurityController;
import org.tedros.server.entity.ITUser;
import org.tedros.server.security.TAccessToken;
import org.tedros.server.util.TLoggerUtil;

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

	public TAiTurnResponse interact(TAccessToken token, TAiTurnRequest request) {
		long start = System.currentTimeMillis();
		TAiTurnResponse resp;
		String conversationId = request != null ? request.getConversationId() : null;
		Long userId = null;
		TAiTokenUsage usage = null;
		try {
			if (request == null || request.getType() == null)
				return error(null, ERROR_INVALID_REQUEST, "Request or request type is null");

			if (request.getType() == TAiTurnRequestType.CLOSE) {
				store.remove(conversationId);
				resp = new TAiTurnResponse();
				resp.setType(TAiTurnResponseType.FINAL);
				resp.setConversationId(conversationId);
				return resp;
			}

			TAiRelayConfigSnapshot cfg = config.snapshot();
			if (!cfg.isAiEnabled())
				return error(conversationId, ERROR_AI_DISABLED, "Artificial intelligence is disabled");

			TUser user = resolveUser(token);
			if (user == null)
				return error(conversationId, ERROR_ACCESS_DENIED, "User not found for the given token");
			userId = user.getId();

			resp = switch (request.getType()) {
			case MESSAGE -> onMessage(request, user, cfg);
			case TOOL_RESULTS -> onToolResults(request, user, cfg);
			default -> error(conversationId, ERROR_INVALID_REQUEST, "Unsupported request type");
			};
			conversationId = resp.getConversationId();
			usage = resp.getTokenUsage();
			return resp;
		} catch (Exception e) {
			LOGGER.error("Unexpected error on AI relay interact", e);
			return error(conversationId, ERROR_INTERNAL, e.getMessage());
		} finally {
			LOGGER.info("AI relay turn | user {} | conversation {} | tokens in {} out {} total {} | {} ms",
					userId, conversationId,
					usage != null ? usage.getInput() : null,
					usage != null ? usage.getOutput() : null,
					usage != null ? usage.getTotal() : null,
					System.currentTimeMillis() - start);
		}
	}

	private TAiTurnResponse onMessage(TAiTurnRequest request, TUser user, TAiRelayConfigSnapshot cfg) {

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
			TAiTurnResponse resp = loop.run(conv, model, catalog);
			return finish(resp, conv);
		} finally {
			conv.getLock().unlock();
		}
	}

	private TAiTurnResponse onToolResults(TAiTurnRequest request, TUser user, TAiRelayConfigSnapshot cfg) {

		TAiConversation conv = requireConversation(request.getConversationId(), user);
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
			TAiTurnResponse resp = loop.resume(conv, model, catalog, request.getToolResults());
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
