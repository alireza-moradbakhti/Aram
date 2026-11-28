package com.aram.feature.dashboard.presentation

import com.aram.core.model.Transaction
import com.aram.core.model.TransactionType
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrendMathTest {

    private val tehran: ZoneId = ZoneId.of("Asia/Tehran")
    private val november = YearMonth.of(2026, 11)

    @Test
    fun `always emits six buckets oldest first ending at the given month`() {
        val buckets = buildMonthlyTrend(emptyList(), november, ZoneOffset.UTC)

        assertEquals(6, buckets.size)
        assertEquals(listOf("Jun", "Jul", "Aug", "Sep", "Oct", "Nov"), buckets.map { it.label })
        assertTrue(buckets.all { it.incomeMinor == 0L && it.expenseMinor == 0L })
    }

    @Test
    fun `missing months stay as zero buckets`() {
        val rows = listOf(
            transaction(TransactionType.EXPENSE, 120_000, at(2026, 7, 10, 12, 0).toInstant().toEpochMilli()),
        )

        val buckets = buildMonthlyTrend(rows, november, tehran)

        assertEquals(listOf("Jun", "Jul", "Aug", "Sep", "Oct", "Nov"), buckets.map { it.label })
        assertEquals(listOf(0L, 120_000L, 0L, 0L, 0L, 0L), buckets.map { it.expenseMinor })
        assertFalse(TrendUiState(buckets).isEmpty)
    }

    @Test
    fun `rows are assigned by the wall clock of the given zone`() {
        val instant = ZonedDateTime.of(2026, 10, 31, 21, 0, 0, 0, ZoneOffset.UTC).toInstant()
        val rows = listOf(transaction(TransactionType.EXPENSE, 50_000, instant.toEpochMilli()))

        // 21:00 UTC is 00:30 on 1 November in Tehran.
        val tehranBuckets = buildMonthlyTrend(rows, november, tehran)
        val utcBuckets = buildMonthlyTrend(rows, november, ZoneOffset.UTC)

        assertEquals(50_000L, tehranBuckets.last().expenseMinor)
        assertEquals(0L, tehranBuckets[4].expenseMinor)
        assertEquals(50_000L, utcBuckets[4].expenseMinor)
        assertEquals(0L, utcBuckets.last().expenseMinor)
    }

    @Test
    fun `month boundaries follow the zone midnight`() {
        val before = at(2026, 10, 1, 0, 0).minusSeconds(1)
        val atMidnight = at(2026, 10, 1, 0, 0)
        val rows = listOf(
            transaction(TransactionType.EXPENSE, 10_000, before.toInstant().toEpochMilli()),
            transaction(TransactionType.EXPENSE, 20_000, atMidnight.toInstant().toEpochMilli()),
        )

        val buckets = buildMonthlyTrend(rows, november, tehran)

        assertEquals(10_000L, buckets[3].expenseMinor) // September
        assertEquals(20_000L, buckets[4].expenseMinor) // October
    }

    @Test
    fun `income and expense are summed separately and transfers are ignored`() {
        val rows = listOf(
            transaction(TransactionType.EXPENSE, 300_000, at(2026, 11, 5, 9, 0).toInstant().toEpochMilli()),
            transaction(TransactionType.EXPENSE, 200_000, at(2026, 11, 20, 18, 0).toInstant().toEpochMilli()),
            transaction(TransactionType.INCOME, 5_000_000, at(2026, 11, 1, 8, 0).toInstant().toEpochMilli()),
            transaction(TransactionType.TRANSFER, 999_999, at(2026, 11, 10, 12, 0).toInstant().toEpochMilli()),
        )

        val buckets = buildMonthlyTrend(rows, november, tehran)

        assertEquals(500_000L, buckets.last().expenseMinor)
        assertEquals(5_000_000L, buckets.last().incomeMinor)
    }

    @Test
    fun `rows outside the window are ignored`() {
        val rows = listOf(
            transaction(TransactionType.EXPENSE, 1_000, at(2026, 5, 31, 23, 0).toInstant().toEpochMilli()), // before June
            transaction(TransactionType.EXPENSE, 2_000, at(2026, 12, 1, 0, 0).toInstant().toEpochMilli()), // after November
        )

        val buckets = buildMonthlyTrend(rows, november, tehran)

        assertTrue(buckets.all { it.incomeMinor == 0L && it.expenseMinor == 0L })
        assertTrue(TrendUiState(buckets).isEmpty)
    }

    @Test
    fun `empty history reports isEmpty`() {
        assertTrue(TrendUiState(buildMonthlyTrend(emptyList(), november, tehran)).isEmpty)
    }

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int) =
        ZonedDateTime.of(LocalDateTime.of(year, month, day, hour, minute), tehran)

    private fun transaction(type: TransactionType, amountMinor: Long, occurredAt: Long) =
        Transaction(
            id = "t-$occurredAt-$type",
            accountId = "acc",
            categoryId = null,
            type = type,
            amountMinor = amountMinor,
            currencyCode = "IRR",
            occurredAt = occurredAt,
            note = null,
            transferGroupId = null,
            recurringRuleId = null,
        )
}