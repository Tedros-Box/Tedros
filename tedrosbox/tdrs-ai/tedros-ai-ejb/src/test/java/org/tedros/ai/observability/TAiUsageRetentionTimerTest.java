package org.tedros.ai.observability;

import static org.junit.Assert.assertEquals;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import org.junit.Test;

/**
 * Testes da logica pura da retencao ({@link TAiUsageRetentionTimer}): a janela
 * do expurgo (alinhada ao 1º dia do mes, para nunca apagar um mes parcial) e o
 * parse da property {@code sys.ai.usage.retention.months}. Ver PR-5.
 */
public class TAiUsageRetentionTimerTest {

	private static Date date(int year, int month, int day) {
		return Date.from(LocalDate.of(year, month, day)
				.atStartOfDay(ZoneId.systemDefault()).toInstant());
	}

	@Test
	public void purgeBoundaryIsFirstDayOfMonthNMonthsAgo() {
		// 2026-07-11, retencao 4 meses → 1º de marco/2026 (marco em diante fica)
		assertEquals(date(2026, 3, 1), TAiUsageRetentionTimer.purgeBoundary(date(2026, 7, 11), 4));
	}

	@Test
	public void purgeBoundaryCrossesYear() {
		// 2026-01-15, retencao 4 meses → 1º de setembro/2025
		assertEquals(date(2025, 9, 1), TAiUsageRetentionTimer.purgeBoundary(date(2026, 1, 15), 4));
	}

	@Test
	public void purgeBoundaryIgnoresDayOfMonth() {
		// qualquer dia do mes cai no 1º dia do mes-alvo (expurgo por meses inteiros)
		assertEquals(TAiUsageRetentionTimer.purgeBoundary(date(2026, 7, 1), 4),
				TAiUsageRetentionTimer.purgeBoundary(date(2026, 7, 28), 4));
	}

	@Test
	public void parseMonthsUsesValueWhenValid() {
		assertEquals(6, TAiUsageRetentionTimer.parseMonths("6", 4));
		assertEquals(1, TAiUsageRetentionTimer.parseMonths(" 1 ", 4));
	}

	@Test
	public void parseMonthsFallsBackOnInvalidOrNonPositive() {
		assertEquals(4, TAiUsageRetentionTimer.parseMonths(null, 4));
		assertEquals(4, TAiUsageRetentionTimer.parseMonths("", 4));
		assertEquals(4, TAiUsageRetentionTimer.parseMonths("abc", 4));
		assertEquals(4, TAiUsageRetentionTimer.parseMonths("0", 4)); // <1 → default (evita expurgo agressivo)
		assertEquals(4, TAiUsageRetentionTimer.parseMonths("-3", 4));
	}
}
