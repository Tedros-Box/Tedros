package org.tedros.ai.observability.entity;

import java.io.Serializable;
import java.util.Date;

import org.tedros.core.domain.DomainSchema;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

/**
 * Evento de consumo por turno do Tool Relay, gravado de forma assincrona para o
 * drill-down por usuario (objetivos "consumo por usuario" e "tools por usuario"
 * do plano). Vive na PU {@code tedros_core_pu} e e criado por DDL-generation no
 * boot.
 * <p>
 * Portabilidade H2/Postgres: {@code toolCalls} e um VARCHAR grande com o JSON
 * {@code [{name,outcome,ms}]} — evita depender de {@code jsonb} (Postgres-only).
 * No Grafana/Postgres o filtro por tool pode usar {@code jsonb_array_elements}
 * sobre {@code tool_calls::jsonb}.
 * <p>
 * REGRA: este evento e a UNICA fonte com granularidade por usuario. O Prometheus
 * nunca recebe {@code userId} como label (cardinalidade).
 *
 * @author Davis Gordon
 */
@Entity
@Table(name = "TAI_USAGE_EVENT", schema = DomainSchema.tedros_core)
public class TAiUsageEvent implements Serializable {

	private static final long serialVersionUID = 1L;

	/** Limite do JSON de tool calls (VARCHAR portavel). */
	public static final int TOOL_CALLS_MAX = 4000;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ID")
	private Long id;

	// java.util.Date + @Temporal → coluna TIMESTAMP. NAO usar java.time.Instant:
	// o EclipseLink serializa Instant como BLOB/bytea no DDL create-tables.
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "TS")
	private Date ts;

	@Column(name = "USER_ID")
	private Long userId;

	@Column(name = "USER_NAME", length = 255)
	private String userName;

	@Column(name = "PROVIDER", length = 40)
	private String provider;

	@Column(name = "MODEL", length = 120)
	private String model;

	@Column(name = "TURN_TYPE", length = 40)
	private String turnType;

	@Column(name = "INPUT_TOKENS")
	private Long inputTokens;

	@Column(name = "OUTPUT_TOKENS")
	private Long outputTokens;

	@Column(name = "TOTAL_TOKENS")
	private Long totalTokens;

	@Column(name = "TURN_MS")
	private Integer turnMs;

	@Column(name = "TURN_DEPTH")
	private Integer turnDepth;

	@Column(name = "TOOL_CALLS", length = TOOL_CALLS_MAX)
	private String toolCalls;

	@Column(name = "OUTCOME", length = 40)
	private String outcome;

	@Column(name = "ERROR_CODE", length = 60)
	private String errorCode;

	@Column(name = "CONVERSATION_ID", length = 120)
	private String conversationId;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Date getTs() {
		return ts;
	}

	public void setTs(Date ts) {
		this.ts = ts;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public String getTurnType() {
		return turnType;
	}

	public void setTurnType(String turnType) {
		this.turnType = turnType;
	}

	public Long getInputTokens() {
		return inputTokens;
	}

	public void setInputTokens(Long inputTokens) {
		this.inputTokens = inputTokens;
	}

	public Long getOutputTokens() {
		return outputTokens;
	}

	public void setOutputTokens(Long outputTokens) {
		this.outputTokens = outputTokens;
	}

	public Long getTotalTokens() {
		return totalTokens;
	}

	public void setTotalTokens(Long totalTokens) {
		this.totalTokens = totalTokens;
	}

	public Integer getTurnMs() {
		return turnMs;
	}

	public void setTurnMs(Integer turnMs) {
		this.turnMs = turnMs;
	}

	public Integer getTurnDepth() {
		return turnDepth;
	}

	public void setTurnDepth(Integer turnDepth) {
		this.turnDepth = turnDepth;
	}

	public String getToolCalls() {
		return toolCalls;
	}

	public void setToolCalls(String toolCalls) {
		this.toolCalls = toolCalls;
	}

	public String getOutcome() {
		return outcome;
	}

	public void setOutcome(String outcome) {
		this.outcome = outcome;
	}

	public String getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(String errorCode) {
		this.errorCode = errorCode;
	}

	public String getConversationId() {
		return conversationId;
	}

	public void setConversationId(String conversationId) {
		this.conversationId = conversationId;
	}
}
