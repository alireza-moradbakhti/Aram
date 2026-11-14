package com.aram.core.data.defaults

import com.aram.core.database.entity.AccountEntity
import com.aram.core.database.entity.CategoryEntity
import com.aram.core.model.AccountType
import com.aram.core.model.CategoryKind
import java.util.UUID

internal object DefaultData {

    const val DEFAULT_CURRENCY = "USD"

    fun categories(userId: String, now: Long): List<CategoryEntity> = listOf(
        category(userId, "Salary", CategoryKind.INCOME, "salary", 0xFF059669, 0, now),
        category(userId, "Freelance", CategoryKind.INCOME, "freelance", 0xFF10B981, 1, now),
        category(userId, "Food & Drinks", CategoryKind.EXPENSE, "food", 0xFFEF5350, 0, now),
        category(userId, "Groceries", CategoryKind.EXPENSE, "groceries", 0xFFF97316, 1, now),
        category(userId, "Transport", CategoryKind.EXPENSE, "transport", 0xFF3B82F6, 2, now),
        category(userId, "Rent", CategoryKind.EXPENSE, "home", 0xFF8B5CF6, 3, now),
        category(userId, "Utilities", CategoryKind.EXPENSE, "utilities", 0xFF06B6D4, 4, now),
        category(userId, "Shopping", CategoryKind.EXPENSE, "shopping", 0xFFEC4899, 5, now),
        category(userId, "Health", CategoryKind.EXPENSE, "health", 0xFF14B8A6, 6, now),
        category(userId, "Entertainment", CategoryKind.EXPENSE, "fun", 0xFFF59E0B, 7, now),
        category(userId, "Subscriptions", CategoryKind.EXPENSE, "subscriptions", 0xFF6366F1, 8, now),
        category(userId, "Other", CategoryKind.EXPENSE, "other", 0xFF64748B, 9, now),
    )

    fun cashAccount(userId: String, now: Long): AccountEntity = AccountEntity(
        id = UUID.randomUUID().toString(),
        userId = userId,
        name = "Cash",
        type = AccountType.CASH.name,
        currencyCode = DEFAULT_CURRENCY,
        openingBalanceMinor = 0L,
        colorArgb = 0xFF0E9F6E.toInt(),
        iconKey = "wallet",
        sortOrder = 0,
        archived = false,
        createdAt = now,
        updatedAt = now,
        deletedAt = null,
    )

    private fun category(
        userId: String,
        name: String,
        kind: CategoryKind,
        iconKey: String,
        color: Long,
        sortOrder: Int,
        now: Long,
    ): CategoryEntity = CategoryEntity(
        id = UUID.randomUUID().toString(),
        userId = userId,
        name = name,
        kind = kind.name,
        parentId = null,
        colorArgb = color.toInt(),
        iconKey = iconKey,
        sortOrder = sortOrder,
        archived = false,
        createdAt = now,
        updatedAt = now,
        deletedAt = null,
    )
}
