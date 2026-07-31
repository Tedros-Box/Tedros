package org.tedros.ai.observability.pricing.entity;

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
 * Tabela de precos do Tool Relay (editavel em runtime), resolvida por
 * {@code (provider, model, tier)}. Vive na PU {@code tedros_core_pu}, schema
 * {@code tedros_core}, criada por DDL-generation no boot e populada pelo
 * {@code TAiPriceSeed} (seed {@code TAI_PRICE_seed.sql}) na primeira subida.
 * <p>
 * Precos em USD por 1.000.000 (1M) de tokens. Ao mudar um preco NAO se edita a
 * linha antiga — cria-se uma nova com {@code effectiveFrom} nova (preserva a
 * auditoria retroativa); a resolucao pega a linha ACTIVE mais recente com
 * {@code effectiveFrom <= ts}.
 * <p>
 * Portabilidade JDBC: {@code java.util.Date} + {@code @Temporal} (nao
 * {@code java.time.Instant}, que o EclipseLink serializaria como BLOB no DDL).
 *
 * @author Davis Gordon
 */
@Entity
@Table(name = "TAI_PRICE", schema = DomainSchema.tedros_core)
public class TAiPrice implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ID")
	private Long id;

	/** Casa com {@code providerLabel(cfg)} — OPENAI/GROK/GEMINI. */
	@Column(name = "PROVIDER", length = 40)
	private String provider;

	/** Casa exatamente com {@code cfg.getModel()}. */
	@Column(name = "MODEL", length = 120)
	private String model;

	/** {@code standard} | {@code long_context}. */
	@Column(name = "TIER", length = 20)
	private String tier;

	/** Entrada sem cache, USD por 1M tokens. */
	@Column(name = "PRICE_IN_USD_1M", precision = 12, scale = 6)
	private BigDecimal priceInUsd1M;

	/** Leitura de cache, USD por 1M tokens. */
	@Column(name = "PRICE_CACHE_USD_1M", precision = 12, scale = 6)
	private BigDecimal priceCacheUsd1M;

	/** Saida, USD por 1M tokens. */
	@Column(name = "PRICE_OUT_USD_1M", precision = 12, scale = 6)
	private BigDecimal priceOutUsd1M;

	/** Fixo {@code USD}. */
	@Column(name = "CURRENCY", length = 8)
	private String currency;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "EFFECTIVE_FROM")
	private Date effectiveFrom;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "EFFECTIVE_TO")
	private Date effectiveTo;

	/** Versao gravada no ledger junto ao custo (ex.: {@code 2026-07}). */
	@Column(name = "VERSION", length = 40)
	private String version;

	@Column(name = "ACTIVE")
	private boolean active;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
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

	public String getTier() {
		return tier;
	}

	public void setTier(String tier) {
		this.tier = tier;
	}

	public BigDecimal getPriceInUsd1M() {
		return priceInUsd1M;
	}

	public void setPriceInUsd1M(BigDecimal priceInUsd1M) {
		this.priceInUsd1M = priceInUsd1M;
	}

	public BigDecimal getPriceCacheUsd1M() {
		return priceCacheUsd1M;
	}

	public void setPriceCacheUsd1M(BigDecimal priceCacheUsd1M) {
		this.priceCacheUsd1M = priceCacheUsd1M;
	}

	public BigDecimal getPriceOutUsd1M() {
		return priceOutUsd1M;
	}

	public void setPriceOutUsd1M(BigDecimal priceOutUsd1M) {
		this.priceOutUsd1M = priceOutUsd1M;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public Date getEffectiveFrom() {
		return effectiveFrom;
	}

	public void setEffectiveFrom(Date effectiveFrom) {
		this.effectiveFrom = effectiveFrom;
	}

	public Date getEffectiveTo() {
		return effectiveTo;
	}

	public void setEffectiveTo(Date effectiveTo) {
		this.effectiveTo = effectiveTo;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}
}
