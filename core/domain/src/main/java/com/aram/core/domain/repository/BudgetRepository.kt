package com.aram.core.domain.repository

import com.aram.core.domain.budget.BudgetDraft
import com.aram.core.domain.budget.BudgetProgress
import com.aram.core.model.Budget
import com.aram.core.model.Outcome
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow

/**
 * Monthly category limits. One budget per user/category/month: upserting the
 * same pair replaces the limit instead of adding a row. Progress compares the
 * limit with the month's expenses in the budget's currency.
 */
interface BudgetRepository {

    /** Budgets for [month] joined with their categories and month spending. */
    fun observeProgress(month: YearMonth): Flow<List<BudgetProgress>>

    suspend fun upsert(draft: BudgetDraft): Outcome<Budget>

    /** Soft-deletes the budget; it stays as a tombstone for future sync. */
    suspend fun delete(id: String): Outcome<Unit>
}