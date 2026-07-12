package org.tedros.ai.toolrelay.function;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.tedros.ai.observability.pricing.TAiCallUsage;
import org.tedros.core.security.model.TUser;
import org.tedros.server.security.TAccessToken;

/**
 * Contexto de execucao de uma tool de backend: usuario autenticado da
 * conversa e o {@link TAccessToken} do request CORRENTE — tools que chamam
 * controllers seguros ({@code TSecureEjbController}) devem usar este token,
 * nunca um token guardado do inicio da conversa (tokens expiram).
 * <p>
 * Tambem serve de bloco de rascunho por-interacao para a observabilidade: uma
 * instancia nova e criada a cada {@code interact()}, entao {@link #getToolCalls()}
 * reflete exatamente as tools chamadas neste turno (usado no evento de consumo
 * por usuario).
 *
 * @author Davis Gordon
 */
public class TAiToolContext {

	/** Uma chamada de tool observada no turno: nome, desfecho e duracao (ms). */
	public record ToolCall(String name, String outcome, long ms) {
	}

	private final TUser user;
	private final TAccessToken token;
	private final List<ToolCall> toolCalls = new ArrayList<>();

	// identidade do turno e snapshot do billing deste interact (Parte 4). O
	// snapshot e tirado sob o lock da conversa (em finish) e lido depois no
	// emitUsageEvent (fora do lock) sem corrida — o ctx e local ao request.
	private String turnId;
	private List<TAiCallUsage> billedCalls = Collections.emptyList();

	public TAiToolContext(TUser user, TAccessToken token) {
		this.user = user;
		this.token = token;
	}

	public TUser getUser() {
		return user;
	}

	public TAccessToken getToken() {
		return token;
	}

	/** Registra uma chamada de tool deste turno (para o drill-down por usuario). */
	public void addToolCall(String name, String outcome, long ms) {
		toolCalls.add(new ToolCall(name, outcome, ms));
	}

	public List<ToolCall> getToolCalls() {
		return toolCalls;
	}

	public String getTurnId() {
		return turnId;
	}

	public void setTurnId(String turnId) {
		this.turnId = turnId;
	}

	/** Chamadas ao LLM faturadas neste interact (snapshot tirado sob o lock). */
	public List<TAiCallUsage> getBilledCalls() {
		return billedCalls;
	}

	public void setBilledCalls(List<TAiCallUsage> billedCalls) {
		this.billedCalls = billedCalls != null ? billedCalls : Collections.emptyList();
	}
}
