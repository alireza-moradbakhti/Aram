package com.aram.core.domain.budget

import java.time.YearMonth

/**
 * A per-category monthly limit as entered by the user. The repository owns the
 * id, timestamps, and tombstone; exactly one budget exists per
 * user/category/month, so an upsert for the same pair replaces the limit.
 */
data class BudgetDraft(
    val categoryId: String,
    val month: YearMonth,
    val limitMinor: Long,
    val currencyCode: String,
)