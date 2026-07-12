package org.tedros.ai.observability.entity;

import java.io.Serializable;
import java.math.BigDecimal;
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
 * Ledger de custo do Tool Relay — <b>fonte de verdade</b> do billing, uma linha
 * <b>por chamada</b> ao LLM (nao por turno). Um turno faz N chamadas (recursao
 * de tools) e gera N linhas com o mesmo {@code turnId}. Vive na PU
 * {@code tedros_core_pu}, schema {@code tedros_core}, criada por DDL-generation.
 * <p>
 * O consumo por usuario/provider/modelo sai daqui via SQL (Grafana/Postgres) —
 * o Prometheus nunca recebe {@code userId} como label. {@code TOKENS_OUT} ja e o
 * valor faturavel (ver Parte 3); {@code TOKENS_REASONING} e so auditoria.
 *
 * @author Davis Gordon
 */
@Entity
@Table(name = "TAI_LLM_CALL", schema = DomainSchema.tedros_core)
public class TAiLlmCall implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ID")
	private Long id;

	// java.util.Date + @Temporal → TIMESTAMP (nao usar java.time.Instant: vira BLOB no DDL)
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "TS")
	private Date ts;

	@Column(name = "USER_ID")
	private Long userId;

	@Column(name = "USER_NAME", length = 255)
	private String userName;

	@Column(name = "CONVERSATION_ID", length = 120)
	private String conversationId;

	@Column(name = "TURN_ID", length = 60)
	private String turnId;

	/** Indice da chamada no turno (0-based). */
	@Column(name = "CALL_INDEX")
	private Integer callIndex;

	@Column(name = "PROVIDER", length = 40)
	private String provider;

	@Column(name = "MODEL", length = 120)
	private String model;

	@Column(name = "CONTEXT_TIER", length = 20)
	private String contextTier;

	/** Entrada sem cache (uncached). */
	@Column(name = "TOKENS_IN_FULL")
	private Long tokensInFull;

	@Column(name = "TOKENS_IN_CACHE")
	private Long tokensInCache;

	/** Saida faturavel (ver Parte 3). */
	@Column(name = "TOKENS_OUT")
	private Long tokensOut;

	/** Reasoning/thoughts — auditoria. */
	@Column(name = "TOKENS_REASONING")
	private Long tokensReasoning;

	@Column(name = "LLM_MS")
	private Integer llmMs;

	@Column(name = "COST_USD", precision = 12, scale = 6)
	private BigDecimal costUsd;

	@Column(name = "PRICE_VERSION", length = 40)
	private String priceVersion;

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

	public String getConversationId() {
		return conversationId;
	}

	public void setConversationId(String conversationId) {
		this.conversationId = conversationId;
	}

	public String getTurnId() {
		return turnId;
	}

	public void setTurnId(String turnId) {
		this.turnId = turnId;
	}

	public Integer getCallIndex() {
		return callIndex;
	}

	public void setCallIndex(Integer callIndex) {
		this.callIndex = callIndex;
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

	public String getContextTier() {
		return contextTier;
	}

	public void setContextTier(String contextTier) {
		this.contextTier = contextTier;
	}

	public Long getTokensInFull() {
		return tokensInFull;
	}

	public void setTokensInFull(Long tokensInFull) {
		this.tokensInFull = tokensInFull;
	}

	public Long getTokensInCache() {
		return tokensInCache;
	}

	public void setTokensInCache(Long tokensInCache) {
		this.tokensInCache = tokensInCache;
	}

	public Long getTokensOut() {
		return tokensOut;
	}

	public void setTokensOut(Long tokensOut) {
		this.tokensOut = tokensOut;
	}

	public Long getTokensReasoning() {
		return tokensReasoning;
	}

	public void setTokensReasoning(Long tokensReasoning) {
		this.tokensReasoning = tokensReasoning;
	}

	public Integer getLlmMs() {
		return llmMs;
	}

	public void setLlmMs(Integer llmMs) {
		this.llmMs = llmMs;
	}

	public BigDecimal getCostUsd() {
		return costUsd;
	}

	public void setCostUsd(BigDecimal costUsd) {
		this.costUsd = costUsd;
	}

	public String getPriceVersion() {
		return priceVersion;
	}

	public void setPriceVersion(String priceVersion) {
		this.priceVersion = priceVersion;
	}
}
