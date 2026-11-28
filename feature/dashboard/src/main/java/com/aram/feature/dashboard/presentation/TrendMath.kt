package com.aram.feature.dashboard.presentation

import com.aram.core.model.Transaction
import com.aram.core.model.TransactionType
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class MonthBucket(
    val label: String,
    val incomeMinor: Long,
    val expenseMinor: Long,
)

data class TrendUiState(
    val buckets: List<MonthBucket> = emptyList(),
) {
    val isEmpty: Boolean
        get() = buckets.all { it.incomeMinor == 0L && it.expenseMinor == 0L }
}

/**
 * Buckets [rows] into the [months] calendar months ending at [endMonth],
 * oldest first. Months without rows are emitted with zero totals so the chart
 * never shows gaps. Rows are assigned to months by their instant in [zone].
 */
internal fun buildMonthlyTrend(
    rows: List<Transaction>,
    endMonth: YearMonth,
    zone: ZoneId,
    months: Int = 6,
): List<MonthBucket> {
    require(months > 0) { "months must be positive" }
    val firstMonth = endMonth.minusMonths((months - 1).toLong())
    val income = LongArray(months)
    val expense = LongArray(months)

    for (row in rows) {
        val month = Instant.ofEpochMilli(row.occurredAt).atZone(zone).toLocalDate().let(YearMonth::from)
        if (month.isBefore(firstMonth) || month.isAfter(endMonth)) continue
        val index = firstMonth.until(month, java.time.temporal.ChronoUnit.MONTHS).toInt()
        when (row.type) {
            TransactionType.INCOME -> income[index] += row.amountMinor
            TransactionType.EXPENSE -> expense[index] += row.amountMinor
            TransactionType.TRANSFER -> Unit
        }
    }

    return (0 until months).map { index ->
        MonthBucket(
            label = MONTH_FORMAT.format(firstMonth.plusMonths(index.toLong())),
            incomeMinor = income[index],
            expenseMinor = expense[index],
        )
    }
}

private val MONTH_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM", Locale.US)