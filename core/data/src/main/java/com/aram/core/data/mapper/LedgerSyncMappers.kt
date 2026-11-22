package com.aram.core.data.mapper

import com.aram.core.database.entity.AccountEntity
import com.aram.core.database.entity.CategoryEntity
import com.aram.core.database.entity.TransactionEntity
import com.aram.core.model.AccountType
import com.aram.core.model.CategoryKind
import com.aram.core.model.TransactionType
import com.aram.core.network.dto.AccountDto
import com.aram.core.network.dto.CategoryRefDto
import com.aram.core.network.dto.TransactionDto
import java.time.Instant
import java.time.OffsetDateTime
import kotlin.math.roundToLong

/** Account id used when the server row carries no account reference. */
internal const val UNASSIGNED_ACCOUNT_ID = ""

internal fun CategoryRefDto.toEntity(userId: String, now: Long, sortOrder: Int): CategoryEntity =
    CategoryEntity(
        id = id,
        userId = userId,
        name = name,
        kind = if (kind.equals("income", ignoreCase = true)) {
            CategoryKind.INCOME.name
        } else {
            CategoryKind.EXPENSE.name
        },
        parentId = null,
        colorArgb = toneColor(tone),
        iconKey = normalizedCategoryIcon(iconKey),
        sortOrder = sortOrder,
        archived = false,
        createdAt = now,
        updatedAt = now,
        deletedAt = null,
        slug = slug,
    )

internal fun AccountDto.toEntity(
    userId: String,
    currencyCode: String,
    now: Long,
    sortOrder: Int,
): AccountEntity = AccountEntity(
    id = id,
    userId = userId,
    name = name,
    type = accountType(kind),
    currencyCode = currencyCode,
    openingBalanceMinor = 0L,
    colorArgb = accountColor(kind),
    iconKey = kind,
    sortOrder = sortOrder,
    archived = false,
    createdAt = now,
    updatedAt = now,
    deletedAt = null,
)

/** Null when the ledger amount or timestamp cannot be understood. */
internal fun TransactionDto.toEntity(userId: String, now: Long): TransactionEntity? {
    val occurred = parseInstantMillis(occurredAt) ?: return null
    return TransactionEntity(
        id = id,
        userId = userId,
        type = if (kind.equals("income", ignoreCase = true)) {
            TransactionType.INCOME.name
        } else {
            TransactionType.EXPENSE.name
        },
        accountId = account?.id ?: UNASSIGNED_ACCOUNT_ID,
        categoryId = category.id,
        amountMinor = amountIrr.roundToLong(),
        currencyCode = "IRR",
        occurredAt = occurred,
        note = note?.takeIf { it.isNotBlank() },
        transferGroupId = null,
        recurringRuleId = null,
        createdAt = now,
        updatedAt = now,
        deletedAt = null,
        pendingPush = false,
    )
}

internal fun parseInstantMillis(value: String): Long? =
    runCatching { Instant.parse(value).toEpochMilli() }
        .recoverCatching { OffsetDateTime.parse(value).toInstant().toEpochMilli() }
        .getOrNull()

private fun normalizedCategoryIcon(iconKey: String): String = when (iconKey) {
    "cart" -> "groceries"
    "car" -> "transport"
    "music" -> "subscriptions"
    "receipt" -> "utilities"
    "bag" -> "shopping"
    "smile" -> "fun"
    "in" -> "salary"
    "food", "groceries", "transport", "home", "utilities", "shopping",
    "health", "fun", "subscriptions", "salary", "freelance", "wallet",
    -> iconKey
    else -> "receipt"
}

private fun toneColor(tone: String): Int = when (tone.lowercase()) {
    "ok" -> 0xFF059669.toInt()
    "warn" -> 0xFFF59E0B.toInt()
    "info" -> 0xFF3B82F6.toInt()
    "danger" -> 0xFFEF4444.toInt()
    else -> 0xFF64748B.toInt()
}

private fun accountType(kind: String): String = when (kind.lowercase()) {
    "cash" -> AccountType.CASH.name
    "card" -> AccountType.BANK.name
    "auto" -> AccountType.WALLET.name
    else -> AccountType.CASH.name
}

private fun accountColor(kind: String): Int = when (kind.lowercase()) {
    "cash" -> 0xFF0E9F6E.toInt()
    "card" -> 0xFF3B82F6.toInt()
    "auto" -> 0xFF8B5CF6.toInt()
    else -> 0xFF64748B.toInt()
}
