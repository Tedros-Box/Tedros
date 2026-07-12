package org.tedros.ai.observability.pricing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.tedros.ai.observability.TAiMetrics;
import org.tedros.ai.observability.pricing.entity.TAiPrice;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * Testes da precificacao por chamada ({@link TAiPriceBook}): custo conferido a
 * mao para OpenAI/Grok/Gemini, tier {@code long_context} aplicado e com fallback
 * para {@code standard}, e preco ausente → {@code null} + metrica. Ver PR-2.
 */
public class TAiPriceBookTest {

	private SimpleMeterRegistry reg;
	private TAiPriceBook book;

	@Before
	public void setUp() {
		reg = new SimpleMeterRegistry();
		book = new TAiPriceBook();
		book.metrics = new TAiMetrics(reg);
		book.priceEAO = null; // primeForTest injeta o cache; o EAO nao e tocado
	}

	private static TAiPrice price(String provider, String model, String tier,
			String in, String cache, String out) {
		TAiPrice p = new TAiPrice();
		p.setProvider(provider);
		p.setModel(model);
		p.setTier(tier);
		p.setPriceInUsd1M(new BigDecimal(in));
		p.setPriceCacheUsd1M(new BigDecimal(cache));
		p.setPriceOutUsd1M(new BigDecimal(out));
		p.setCurrency("USD");
		p.setEffectiveFrom(new Date(0L)); // epoch → sempre vigente
		p.setVersion("2026-07");
		p.setActive(true);
		return p;
	}

	private static TAiCallUsage call(String provider, String model, String tier,
			long uncached, long cached, long output) {
		TAiCallUsage c = new TAiCallUsage();
		c.setProvider(provider);
		c.setModel(model);
		c.setTier(tier);
		c.setInputUncached(uncached);
		c.setInputCached(cached);
		c.setOutput(output);
		return c;
	}

	@Test
	public void openAiCostMatchesManualCalc() {
		book.primeForTest(List.of(price("OPENAI", "gpt-5.1-2025-11-13", "standard", "1.25", "0.125", "10.00")));
		// (70*1.25 + 30*0.125 + 50*10.00)/1M = (87.5 + 3.75 + 500)/1M = 591.25/1M = 0.00059125
		TAiCallUsage c = call("OPENAI", "gpt-5.1-2025-11-13", "standard", 70, 30, 50);

		BigDecimal cost = book.cost(c);
		assertEquals(0, new BigDecimal("0.000591").compareTo(cost));
		assertEquals("2026-07", c.getPriceVersion());
	}

	@Test
	public void grokCostMatchesManualCalc() {
		book.primeForTest(List.of(price("GROK", "grok-4-fast-reasoning", "standard", "0.20", "0.05", "0.50")));
		// (1000*0.20 + 100*0.05 + 200*0.50)/1M = (200 + 5 + 100)/1M = 305/1M = 0.000305
		TAiCallUsage c = call("GROK", "grok-4-fast-reasoning", "standard", 1000, 100, 200);

		assertEquals(0, new BigDecimal("0.000305").compareTo(book.cost(c)));
	}

	@Test
	public void geminiLongContextTierApplied() {
		book.primeForTest(List.of(
				price("GEMINI", "gemini-2.5-pro", "standard", "1.25", "0.125", "10.00"),
				price("GEMINI", "gemini-2.5-pro", "long_context", "2.50", "0.25", "15.00")));
		// tier long_context: (250000*2.50 + 0 + 100*15.00)/1M = (625000 + 1500)/1M = 0.626500
		TAiCallUsage c = call("GEMINI", "gemini-2.5-pro", "long_context", 250_000, 0, 100);

		assertEquals(0, new BigDecimal("0.626500").compareTo(book.cost(c)));
	}

	@Test
	public void tierFallsBackToStandardWhenLongContextAbsent() {
		// modelo flat: so tem linha standard; pedido long_context cai para standard
		book.primeForTest(List.of(price("GEMINI", "gemini-3.5-flash", "standard", "1.50", "0.15", "9.00")));
		TAiCallUsage c = call("GEMINI", "gemini-3.5-flash", "long_context", 1000, 0, 100);

		// (1000*1.50 + 100*9.00)/1M = (1500 + 900)/1M = 0.002400 (preco standard aplicado)
		assertEquals(0, new BigDecimal("0.002400").compareTo(book.cost(c)));
	}

	@Test
	public void missingPriceReturnsNullAndRecordsMetric() {
		book.primeForTest(List.of(price("OPENAI", "gpt-5.1-2025-11-13", "standard", "1.25", "0.125", "10.00")));
		TAiCallUsage c = call("OPENAI", "ghost-model", "standard", 100, 0, 50);

		assertNull(book.cost(c));
		assertNull(c.getPriceVersion());
		assertEquals(1.0, reg.get("tedros_ai_pricing_missing_total")
				.tag("provider", "OPENAI").tag("model", "ghost-model").tag("tier", "standard")
				.counter().count(), 0.0001);
	}

	@Test
	public void mostRecentEffectiveFromWins() {
		TAiPrice old = price("OPENAI", "gpt-5.1", "standard", "1.00", "0.10", "8.00");
		old.setEffectiveFrom(new Date(0L));
		old.setVersion("2026-01");
		TAiPrice recent = price("OPENAI", "gpt-5.1", "standard", "1.25", "0.125", "10.00");
		recent.setEffectiveFrom(new Date()); // agora
		recent.setVersion("2026-07");
		book.primeForTest(List.of(old, recent));

		TAiCallUsage c = call("OPENAI", "gpt-5.1", "standard", 100, 0, 50);
		// usa a linha mais recente: (100*1.25 + 50*10.00)/1M = (125 + 500)/1M = 0.000625
		assertEquals(0, new BigDecimal("0.000625").compareTo(book.cost(c)));
		assertEquals("2026-07", c.getPriceVersion());
	}

	@Test
	public void futureEffectiveFromIsNotYetValid() {
		TAiPrice future = price("OPENAI", "gpt-5.1", "standard", "1.25", "0.125", "10.00");
		future.setEffectiveFrom(new Date(System.currentTimeMillis() + 86_400_000L)); // amanha
		book.primeForTest(List.of(future));

		// unica linha ainda nao vigente → sem preco
		assertNull(book.cost(call("OPENAI", "gpt-5.1", "standard", 100, 0, 50)));
	}
}
