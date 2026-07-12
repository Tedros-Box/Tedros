package org.tedros.ai.observability.pricing;

import java.math.BigDecimal;

/**
 * Uso e custo de UMA chamada ao LLM (grao de billing do ledger). Um turno faz N
 * chamadas (recursao de tools, ate {@code MAX_RECURSION_DEPTH}); cada uma gera um
 * {@code TAiCallUsage}.
 * <p>
 * Regras (ver Parte 3 do plano):
 * <ul>
 *   <li>{@code output} ja e o valor <b>faturavel</b> — para Gemini os
 *       {@code thoughts} ja estao somados aqui; para OpenAI/Grok o
 *       {@code reasoning} ja esta incluso no output do provedor (nao somar).</li>
 *   <li>{@code reasoning} e mantido apenas para <b>auditoria</b>.</li>
 *   <li>{@code inputUncached} nunca e negativo.</li>
 *   <li>{@code costUsd} e {@code priceVersion} sao preenchidos depois pelo
 *       {@code TAiPriceBook} (ficam nulos ate a precificacao).</li>
 * </ul>
 *
 * @author Davis Gordon
 */
public class TAiCallUsage {

	/** Tier padrao (sem tiering por tamanho de prompt). */
	public static final String TIER_STANDARD = "standard";

	/** Tier de contexto longo (Gemini Pro com prompt &gt; 200k tokens). */
	public static final String TIER_LONG_CONTEXT = "long_context";

	private int callIndex;
	private String provider;
	private String model;
	private String tier;
	private long inputUncached;
	private long inputCached;
	private long output;
	private long reasoning;
	private long llmMs;
	private BigDecimal costUsd;
	private String priceVersion;

	public int getCallIndex() {
		return callIndex;
	}

	public void setCallIndex(int callIndex) {
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

	public String getTier() {
		return tier;
	}

	public void setTier(String tier) {
		this.tier = tier;
	}

	public long getInputUncached() {
		return inputUncached;
	}

	public void setInputUncached(long inputUncached) {
		this.inputUncached = inputUncached;
	}

	public long getInputCached() {
		return inputCached;
	}

	public void setInputCached(long inputCached) {
		this.inputCached = inputCached;
	}

	public long getOutput() {
		return output;
	}

	public void setOutput(long output) {
		this.output = output;
	}

	public long getReasoning() {
		return reasoning;
	}

	public void setReasoning(long reasoning) {
		this.reasoning = reasoning;
	}

	public long getLlmMs() {
		return llmMs;
	}

	public void setLlmMs(long llmMs) {
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

	@Override
	public String toString() {
		return "TAiCallUsage {" +
				" callIndex = " + callIndex +
				", provider = " + provider +
				", model = " + model +
				", tier = " + tier +
				", inputUncached = " + inputUncached +
				", inputCached = " + inputCached +
				", output = " + output +
				", reasoning = " + reasoning +
				", llmMs = " + llmMs +
				", costUsd = " + costUsd +
				", priceVersion = " + priceVersion +
				" }";
	}
}
