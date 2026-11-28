package com.aram.core.domain.safetospend

import com.aram.core.money.Money
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth

data class SafeToSpendBreakdown(
    val income: Money,
    val spent: Money,
    val plannedBudgets: Money,
    val upcomingBills: Money,
    val clampAdjustment: Money,
    val total: Money,
) {
    fun sum(): Money = income + spent + plannedBudgets + upcomingBills + clampAdjustment
}

data class SafeToSpend(
    val total: Money,
    val daysRemaining: Int,
    val dailyAmount: Money,
    val breakdown: SafeToSpendBreakdown,
)

class SafeToSpendCalculator(private val clock: Clock) {

    fun calculate(
        month: YearMonth,
        income: Money,
        spent: Money,
        plannedBudgets: Money,
        upcomingBills: Money,
    ): SafeToSpend {
        val currency = income.currency
        require(
            setOf(spent.currency, plannedBudgets.currency, upcomingBills.currency) == setOf(currency),
        ) { "All amounts must share the ${currency.isoCode} currency" }

        val daysRemaining = daysRemainingIn(month)
        val rawMinor = income.amountMinor - spent.amountMinor -
            plannedBudgets.amountMinor - upcomingBills.amountMinor
        val totalMinor = rawMinor.coerceAtLeast(0L)

        val breakdown = SafeToSpendBreakdown(
            income = income,
            spent = Money(-spent.amountMinor, currency),
            plannedBudgets = Money(-plannedBudgets.amountMinor, currency),
            upcomingBills = Money(-upcomingBills.amountMinor, currency),
            clampAdjustment = Money(totalMinor - rawMinor, currency),
            total = Money(totalMinor, currency),
        )
        return SafeToSpend(
            total = breakdown.total,
            daysRemaining = daysRemaining,
            dailyAmount = Money(totalMinor / daysRemaining, currency),
            breakdown = breakdown,
        )
    }

    private fun daysRemainingIn(month: YearMonth): Int {
        val today = LocalDate.now(clock)
        val last = month.atEndOfMonth()
        val effective = today.coerceIn(month.atDay(1), last)
        return (last.toEpochDay() - effective.toEpochDay()).toInt() + 1
    }
}