package com.aram.core.domain.budget

import com.aram.core.model.Budget
import com.aram.core.model.Category

/**
 * A budget, its category, and the expense total recorded in the budget's
 * currency inside the budget's month. Spending in another currency is not
 * converted and therefore does not count.
 */
data class BudgetProgress(
    val budget: Budget,
    val category: Category,
    val spentMinor: Long,
) {
    val limitMinor: Long get() = budget.limitMinor

    val remainingMinor: Long get() = (budget.limitMinor - spentMinor).coerceAtLeast(0L)

    val isOver: Boolean get() = spentMinor > budget.limitMinor

    /** 0..1 for bars and rings; over-limit pins at 1. */
    val fraction: Float
        get() = if (budget.limitMinor <= 0L) {
            0f
        } else {
            (spentMinor.toDouble() / budget.limitMinor.toDouble())
                .coerceIn(0.0, 1.0)
                .toFloat()
        }
}