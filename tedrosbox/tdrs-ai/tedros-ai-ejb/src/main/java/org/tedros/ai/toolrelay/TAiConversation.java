package org.tedros.ai.toolrelay;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import org.tedros.ai.model.TAiClientToolSpec;
import org.tedros.ai.toolrelay.function.TJsonSchemaConverter;
import org.tedros.core.security.model.TUser;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.output.TokenUsage;

/**
 * Estado de uma conversa do relay: memoria de mensagens, specs das tools do
 * cliente, estado do turno pendente e lock por conversa.
 * <p>
 * Todo o estado mutavel (exceto {@code lastAccessAt}) e guardado pelo
 * {@link #getLock() lock}: o service so opera na conversa apos
 * {@code tryLock()} bem-sucedido, portanto conversas diferentes nunca se
 * bloqueiam e turnos concorrentes na mesma conversa sao rejeitados.
 *
 * @author Davis Gordon
 */
public class TAiConversation {

	/** Mesmo valor do FE ({@code LangChainOpenAITerosService.MEMORY_WINDOW_SIZE}). */
	public static final int MEMORY_WINDOW_SIZE = 20;

	/**
	 * Estado do turno suspenso aguardando resultados de tools do frontend.
	 */
	public static class PendingTurn {

		private final List<ToolExecutionRequest> requests;
		private final Map<String, ToolExecutionResultMessage> backendResults;
		private final boolean backendRevert;
		private final Set<String> pendingCallIds;
		private final long createdAt;

		public PendingTurn(List<ToolExecutionRequest> requests,
				Map<String, ToolExecutionResultMessage> backendResults,
				boolean backendRevert, Set<String> pendingCallIds) {
			this.requests = List.copyOf(requests);
			this.backendResults = new LinkedHashMap<>(backendResults);
			this.backendRevert = backendRevert;
			this.pendingCallIds = new LinkedHashSet<>(pendingCallIds);
			this.createdAt = System.currentTimeMillis();
		}

		public List<ToolExecutionRequest> getRequests() {
			return requests;
		}

		public Map<String, ToolExecutionResultMessage> getBackendResults() {
			return backendResults;
		}

		public boolean isBackendRevert() {
			return backendRevert;
		}

		public Set<String> getPendingCallIds() {
			return pendingCallIds;
		}

		public long getCreatedAt() {
			return createdAt;
		}

		public boolean isExpired(int ttlMinutes) {
			return (System.currentTimeMillis() - createdAt) > ttlMinutes * 60_000L;
		}
	}

	private final String id;
	private final TUser user;
	private final MessageWindowChatMemory memory;
	private final Map<String, TAiClientToolSpec> clientSpecsByName = new LinkedHashMap<>();
	private final List<ToolSpecification> clientToolSpecifications = new ArrayList<>();
	private final ReentrantLock lock = new ReentrantLock();
	private final long createdAt;
	private volatile long lastAccessAt;

	// Estado do turno corrente — guardado pelo lock
	private PendingTurn pendingTurn;
	private int turnDepth;
	private TokenUsage turnTokenUsage;
	private String lastAiText;

	public TAiConversation(String id, TUser user) {
		this.id = id;
		this.user = user;
		this.memory = MessageWindowChatMemory.withMaxMessages(MEMORY_WINDOW_SIZE);
		this.createdAt = System.currentTimeMillis();
		this.lastAccessAt = createdAt;
	}

	/**
	 * Registra as specs das tools do cliente (1º MESSAGE da conversa),
	 * convertendo o JSON Schema para {@link ToolSpecification} uma unica vez.
	 */
	public void registerClientSpecs(List<TAiClientToolSpec> specs) {
		if (specs == null)
			return;
		for (TAiClientToolSpec spec : specs) {
			if (spec.getName() == null || clientSpecsByName.containsKey(spec.getName()))
				continue;
			clientSpecsByName.put(spec.getName(), spec);
			clientToolSpecifications.add(ToolSpecification.builder()
					.name(spec.getName())
					.description(spec.getDescription())
					.parameters(TJsonSchemaConverter.fromJson(spec.getParametersSchemaJson()))
					.build());
		}
	}

	public String getId() {
		return id;
	}

	public TUser getUser() {
		return user;
	}

	public Long getUserId() {
		return user != null ? user.getId() : null;
	}

	public MessageWindowChatMemory getMemory() {
		return memory;
	}

	public List<ToolSpecification> getClientToolSpecifications() {
		return clientToolSpecifications;
	}

	public boolean hasClientTool(String name) {
		return name != null && clientSpecsByName.containsKey(name);
	}

	public ReentrantLock getLock() {
		return lock;
	}

	public long getCreatedAt() {
		return createdAt;
	}

	public long getLastAccessAt() {
		return lastAccessAt;
	}

	public void touch() {
		this.lastAccessAt = System.currentTimeMillis();
	}

	public boolean isIdleExpired(int ttlMinutes) {
		return (System.currentTimeMillis() - lastAccessAt) > ttlMinutes * 60_000L;
	}

	public PendingTurn getPendingTurn() {
		return pendingTurn;
	}

	public void setPendingTurn(PendingTurn pendingTurn) {
		this.pendingTurn = pendingTurn;
	}

	public boolean hasPendingTurn() {
		return pendingTurn != null;
	}

	public int getTurnDepth() {
		return turnDepth;
	}

	public void incrementTurnDepth() {
		this.turnDepth++;
	}

	/** Zera o estado do turno — chamado a cada novo MESSAGE. */
	public void resetTurn() {
		this.turnDepth = 0;
		this.turnTokenUsage = null;
		this.lastAiText = null;
	}

	public TokenUsage getTurnTokenUsage() {
		return turnTokenUsage;
	}

	public void accumulateTokenUsage(TokenUsage usage) {
		if (usage == null)
			return;
		this.turnTokenUsage = (this.turnTokenUsage == null) ? usage : this.turnTokenUsage.add(usage);
	}

	public String getLastAiText() {
		return lastAiText;
	}

	public void setLastAiText(String lastAiText) {
		this.lastAiText = lastAiText;
	}
}
