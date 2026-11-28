package com.aram.core.data.mapper

import com.aram.core.database.entity.BudgetEntity
import com.aram.core.model.Budget

internal fun BudgetEntity.toModel(): Budget = Budget(
    id = id,
    categoryId = categoryId,
    periodStartEpochDay = periodStartEpochDay,
    limitMinor = limitMinor,
    currencyCode = currencyCode,
)