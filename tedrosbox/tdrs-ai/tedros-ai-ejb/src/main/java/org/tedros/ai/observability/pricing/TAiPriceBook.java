package org.tedros.ai.observability.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.tedros.ai.observability.TAiMetrics;
import org.tedros.ai.observability.pricing.entity.TAiPrice;
import org.tedros.server.util.TLoggerUtil;

import jakarta.ejb.EJB;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Precifica cada chamada ao LLM ({@link TAiCallUsage}) a partir da tabela
 * {@code TAI_PRICE}, mantida em um mapa em memoria com TTL de 5 min. Ver Parte
 * 3.3 do plano.
 * <p>
 * Formula (USD, {@link RoundingMode#HALF_UP}, escala 6):
 * <pre>
 * custo = inputUncached/1M * PRICE_IN
 *       + inputCached  /1M * PRICE_CACHE
 *       + output       /1M * PRICE_OUT
 * </pre>
 * Resolucao: linha ACTIVE de {@code (provider, model, tier)} vigente em {@code ts}
 * ({@code effectiveFrom <= ts < effectiveTo}) com {@code effectiveFrom} mais
 * recente. Sem linha {@code long_context} → cai para {@code standard} (modelos
 * flat). Preco ausente → custo {@code null} + WARN + metrica
 * {@code tedros_ai_pricing_missing_total} (nunca quebra o turno).
 *
 * @author Davis Gordon
 */
@ApplicationScoped
public class TAiPriceBook {

	private static final TLoggerUtil LOGGER = TLoggerUtil.create(TAiPriceBook.class);

	/** TTL do cache em memoria; a UI futura chama {@link #refresh()} apos editar. */
	static final long TTL_MS = 5 * 60_000L;

	private static final BigDecimal MILLION = BigDecimal.valueOf(1_000_000L);
	private static final int SCALE = 6;

	@EJB
	TAiPriceEAO priceEAO;

	@Inject
	TAiMetrics metrics;

	// Mapa provider|model|tier -> linhas; trocado atomicamente a cada (re)carga.
	private volatile Map<String, List<TAiPrice>> pricesByKey = Collections.emptyMap();
	private volatile long loadedAt = 0L;

	/**
	 * Custo (USD) desta chamada, ou {@code null} se nao houver preco. Efeito
	 * colateral: grava {@code priceVersion} da linha resolvida no {@code call}
	 * (evita uma segunda resolucao no chamador). Nunca lanca.
	 */
	public BigDecimal cost(TAiCallUsage call) {
		if (call == null)
			return null;
		try {
			ensureLoaded();
			TAiPrice price = resolve(call.getProvider(), call.getModel(), call.getTier(), new Date());
			if (price == null) {
				LOGGER.warn("No active TAI_PRICE for {}/{}/{} — cost is null",
						call.getProvider(), call.getModel(), call.getTier());
				if (metrics != null)
					metrics.recordPricingMissing(call.getProvider(), call.getModel(), call.getTier());
				return null;
			}
			call.setPriceVersion(price.getVersion());
			return compute(call, price);
		} catch (Exception e) {
			LOGGER.warn("Failed to price call {}/{}: {}", call.getProvider(), call.getModel(), e.getMessage());
			return null;
		}
	}

	/** Forca o recarregamento dos precos na proxima chamada (UI de edicao). */
	public void refresh() {
		loadedAt = 0L;
		load();
	}

	// ------------------------------------------------------------- resolucao

	private BigDecimal compute(TAiCallUsage call, TAiPrice price) {
		BigDecimal cost = BigDecimal.valueOf(call.getInputUncached()).multiply(nz(price.getPriceInUsd1M()))
				.add(BigDecimal.valueOf(call.getInputCached()).multiply(nz(price.getPriceCacheUsd1M())))
				.add(BigDecimal.valueOf(call.getOutput()).multiply(nz(price.getPriceOutUsd1M())));
		return cost.divide(MILLION, SCALE, RoundingMode.HALF_UP);
	}

	/** Resolve com fallback: {@code long_context} sem linha cai para {@code standard}. */
	private TAiPrice resolve(String provider, String model, String tier, Date ts) {
		TAiPrice price = pick(key(provider, model, tier), ts);
		if (price == null && TAiCallUsage.TIER_LONG_CONTEXT.equals(tier))
			price = pick(key(provider, model, TAiCallUsage.TIER_STANDARD), ts);
		return price;
	}

	/** Linha vigente em {@code ts} com {@code effectiveFrom} mais recente. */
	private TAiPrice pick(String key, Date ts) {
		List<TAiPrice> rows = pricesByKey.get(key);
		if (rows == null)
			return null;
		TAiPrice best = null;
		for (TAiPrice r : rows) {
			Date from = r.getEffectiveFrom();
			Date to = r.getEffectiveTo();
			if (from != null && from.after(ts))
				continue; // ainda nao vigente
			if (to != null && !to.after(ts))
				continue; // vigencia encerrada (ts >= effectiveTo)
			if (best == null || isMoreRecent(from, best.getEffectiveFrom()))
				best = r;
		}
		return best;
	}

	/** {@code from} e mais recente que {@code current} (null conta como o mais antigo). */
	private static boolean isMoreRecent(Date from, Date current) {
		if (from == null)
			return false;
		if (current == null)
			return true;
		return from.after(current);
	}

	private static String key(String provider, String model, String tier) {
		return provider + "|" + model + "|" + tier;
	}

	private static BigDecimal nz(BigDecimal v) {
		return v != null ? v : BigDecimal.ZERO;
	}

	// -------------------------------------------------------------- carga

	private void ensureLoaded() {
		if (System.currentTimeMillis() - loadedAt > TTL_MS)
			load();
	}

	private synchronized void load() {
		long now = System.currentTimeMillis();
		if (now - loadedAt <= TTL_MS && !pricesByKey.isEmpty())
			return; // outra thread ja recarregou enquanto esperavamos o lock
		try {
			List<TAiPrice> rows = priceEAO != null ? priceEAO.findActive() : List.of();
			Map<String, List<TAiPrice>> map = new HashMap<>();
			for (TAiPrice r : rows)
				map.computeIfAbsent(key(r.getProvider(), r.getModel(), r.getTier()), k -> new ArrayList<>())
						.add(r);
			pricesByKey = map;
			LOGGER.info("TAI_PRICE loaded: {} active rows, {} keys", rows.size(), map.size());
		} catch (Exception e) {
			// mantem o mapa anterior; nao martela o banco em falhas repetidas
			LOGGER.warn("Failed to (re)load TAI_PRICE, keeping previous prices: {}", e.getMessage());
		} finally {
			loadedAt = now;
		}
	}

	// --------------------------------------------------------------- testes

	/** Seam de teste: injeta precos direto no cache, sem banco. */
	void primeForTest(List<TAiPrice> rows) {
		Map<String, List<TAiPrice>> map = new HashMap<>();
		for (TAiPrice r : rows)
			map.computeIfAbsent(key(r.getProvider(), r.getModel(), r.getTier()), k -> new ArrayList<>()).add(r);
		this.pricesByKey = map;
		this.loadedAt = System.currentTimeMillis();
	}
}
