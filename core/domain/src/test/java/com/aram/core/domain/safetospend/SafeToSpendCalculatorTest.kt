package com.aram.core.domain.safetospend

import com.aram.core.money.CurrencyCode
import com.aram.core.money.Money
import java.time.Clock
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SafeToSpendCalculatorTest {

    private val november = YearMonth.of(2026, 11)

    private fun calculatorOn(day: String) = SafeToSpendCalculator(
        Clock.fixed(Instant.parse("${day}T12:00:00Z"), ZoneOffset.UTC),
    )

    private fun usd(minor: Long) = Money(minor, CurrencyCode.USD)

    private fun assertBreakdownSumsToTotal(result: SafeToSpend) {
        assertEquals(result.total, result.breakdown.sum())
        val remainder = result.total.amountMinor - result.dailyAmount.amountMinor * result.daysRemaining
        assertTrue(remainder in 0 until result.daysRemaining)
    }

    @Test
    fun `mid month splits the pool across the remaining days`() {
        val result = calculatorOn("2026-11-15").calculate(
            month = november,
            income = usd(300_000),
            spent = usd(100_000),
            plannedBudgets = usd(50_000),
            upcomingBills = usd(20_000),
        )

        assertEquals(16, result.daysRemaining)
        assertEquals(usd(130_000), result.total)
        assertEquals(usd(8_125), result.dailyAmount)
        assertEquals(usd(0), result.breakdown.clampAdjustment)
        assertBreakdownSumsToTotal(result)
    }

    @Test
    fun `first day of the month counts every day`() {
        val result = calculatorOn("2026-11-01").calculate(
            month = november,
            income = usd(300_000),
            spent = usd(100_000),
            plannedBudgets = usd(50_000),
            upcomingBills = usd(20_000),
        )

        assertEquals(30, result.daysRemaining)
        assertEquals(usd(4_333), result.dailyAmount)
        assertBreakdownSumsToTotal(result)
    }

    @Test
    fun `last day gives the whole pool for today`() {
        val result = calculatorOn("2026-11-30").calculate(
            month = november,
            income = usd(300_000),
            spent = usd(100_000),
            plannedBudgets = usd(50_000),
            upcomingBills = usd(20_000),
        )

        assertEquals(1, result.daysRemaining)
        assertEquals(usd(130_000), result.dailyAmount)
        assertBreakdownSumsToTotal(result)
    }

    @Test
    fun `overspending clamps the pool and reports the adjustment`() {
        val result = calculatorOn("2026-11-15").calculate(
            month = november,
            income = usd(100_000),
            spent = usd(150_000),
            plannedBudgets = usd(0),
            upcomingBills = usd(0),
        )

        assertEquals(usd(0), result.total)
        assertEquals(usd(0), result.dailyAmount)
        assertEquals(usd(50_000), result.breakdown.clampAdjustment)
        assertBreakdownSumsToTotal(result)
    }

    @Test
    fun `no income leaves nothing to spend`() {
        val result = calculatorOn("2026-11-15").calculate(
            month = november,
            income = usd(0),
            spent = usd(0),
            plannedBudgets = usd(0),
            upcomingBills = usd(25_000),
        )

        assertEquals(usd(0), result.total)
        assertEquals(usd(0), result.dailyAmount)
        assertEquals(usd(25_000), result.breakdown.clampAdjustment)
        assertBreakdownSumsToTotal(result)
    }

    @Test
    fun `zero everything stays deterministic`() {
        val result = calculatorOn("2026-11-15").calculate(
            month = november,
            income = usd(0),
            spent = usd(0),
            plannedBudgets = usd(0),
            upcomingBills = usd(0),
        )

        assertEquals(16, result.daysRemaining)
        assertEquals(usd(0), result.total)
        assertEquals(usd(0), result.dailyAmount)
        assertBreakdownSumsToTotal(result)
    }

    @Test
    fun `a finished month behaves like its last day`() {
        val result = calculatorOn("2026-12-05").calculate(
            month = november,
            income = usd(60_000),
            spent = usd(0),
            plannedBudgets = usd(0),
            upcomingBills = usd(0),
        )

        assertEquals(1, result.daysRemaining)
        assertEquals(usd(60_000), result.dailyAmount)
        assertBreakdownSumsToTotal(result)
    }

    @Test
    fun `a future month counts its full length`() {
        val result = calculatorOn("2026-10-20").calculate(
            month = november,
            income = usd(60_000),
            spent = usd(0),
            plannedBudgets = usd(0),
            upcomingBills = usd(0),
        )

        assertEquals(30, result.daysRemaining)
        assertBreakdownSumsToTotal(result)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `mismatched currencies are rejected`() {
        calculatorOn("2026-11-15").calculate(
            month = november,
            income = usd(100_000),
            spent = Money(50_000, CurrencyCode.EUR),
            plannedBudgets = usd(0),
            upcomingBills = usd(0),
        )
    }
}