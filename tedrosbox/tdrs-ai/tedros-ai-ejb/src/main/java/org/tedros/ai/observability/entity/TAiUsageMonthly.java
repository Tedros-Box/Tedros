package org.tedros.ai.observability.entity;

import java.io.Serializable;
import java.math.BigDecimal;

import org.tedros.core.domain.DomainSchema;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Rollup mensal do consumo do Tool Relay — <b>retencao longa</b> (anos). Uma
 * linha por {@code (user_id, provider, model, year_month)} (chave logica de
 * upsert idempotente), preenchida de madrugada pelo {@code TAiUsageRetentionTimer}
 * a partir do ledger {@link TAiLlmCall} <b>antes</b> do expurgo do detalhe.
 * <p>
 * Nunca sofre expurgo: quando o detalhe do mes e apagado, o total ja congelado
 * aqui sobrevive. Vive na PU {@code tedros_core_pu}, schema {@code tedros_core}.
 *
 * @author Davis Gordon
 */
@Entity
@Table(name = "TAI_USAGE_MONTHLY", schema = DomainSchema.tedros_core)
public class TAiUsageMonthly implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ID")
	private Long id;

	@Column(name = "USER_ID")
	private Long userId;

	@Column(name = "PROVIDER", length = 40)
	private String provider;

	@Column(name = "MODEL", length = 120)
	private String model;

	/** {@code 'YYYY-MM'}. */
	@Column(name = "YEAR_MONTH", length = 7)
	private String yearMonth;

	@Column(name = "SUM_IN_FULL")
	private Long sumInFull;

	@Column(name = "SUM_IN_CACHE")
	private Long sumInCache;

	@Column(name = "SUM_OUT")
	private Long sumOut;

	@Column(name = "SUM_COST_USD", precision = 12, scale = 6)
	private BigDecimal sumCostUsd;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
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

	public String getYearMonth() {
		return yearMonth;
	}

	public void setYearMonth(String yearMonth) {
		this.yearMonth = yearMonth;
	}

	public Long getSumInFull() {
		return sumInFull;
	}

	public void setSumInFull(Long sumInFull) {
		this.sumInFull = sumInFull;
	}

	public Long getSumInCache() {
		return sumInCache;
	}

	public void setSumInCache(Long sumInCache) {
		this.sumInCache = sumInCache;
	}

	public Long getSumOut() {
		return sumOut;
	}

	public void setSumOut(Long sumOut) {
		this.sumOut = sumOut;
	}

	public BigDecimal getSumCostUsd() {
		return sumCostUsd;
	}

	public void setSumCostUsd(BigDecimal sumCostUsd) {
		this.sumCostUsd = sumCostUsd;
	}
}
