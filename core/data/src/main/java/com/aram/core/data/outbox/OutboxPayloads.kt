package com.aram.core.data.outbox

import com.aram.core.database.entity.CategoryEntity
import com.aram.core.database.entity.SyncOutboxEntity
import com.aram.core.database.entity.TransactionEntity
import com.aram.core.network.AramJson
import com.aram.core.network.dto.SyncTransactionItem
import java.time.Instant
import java.util.UUID

internal fun TransactionEntity.toSyncItem(categorySlug: String): SyncTransactionItem =
    SyncTransactionItem(
        id = id,
        kind = type.lowercase(),
        amountIrr = amountMinor.toDouble(),
        categorySlug = categorySlug,
        accountId = accountId.takeIf { it.isNotBlank() },
        note = note,
        occurredAt = Instant.ofEpochMilli(occurredAt).toString(),
        clientUpdatedAt = Instant.ofEpochMilli(updatedAt).toString(),
        enteredCurrency = currencyCode,
        enteredAmount = amountMinor.toDouble(),
        fxRate = null,
    )

/**
 * The server validates the slug against its fixed catalog, so a missing slug
 * falls back to the closest icon-derived value rather than being dropped.
 * A transaction without a category sends a blank slug and lets the API pick
 * the default for its kind.
 */
internal fun categorySlug(categoryId: String?, category: CategoryEntity?): String {
    if (categoryId.isNullOrBlank()) return ""
    return category?.slug ?: category?.let { slugForIcon(it.iconKey) } ?: "other"
}

internal fun slugForIcon(iconKey: String): String = when (iconKey) {
    "groceries" -> "groceries"
    "transport" -> "transport"
    "home" -> "housing"
    "utilities" -> "bills"
    "shopping" -> "shopping"
    "fun" -> "fun"
    "subscriptions" -> "subscriptions"
    "salary", "freelance" -> "salary"
    "food" -> "dining"
    else -> "other"
}

internal fun outboxEntry(
    userId: String,
    entity: TransactionEntity,
    op: String,
    categorySlug: String?,
    now: Long,
): SyncOutboxEntity = SyncOutboxEntity(
    id = UUID.randomUUID().toString(),
    userId = userId,
    entityId = entity.id,
    op = op,
    payload = categorySlug?.let { slug -> AramJson.encodeToString(entity.toSyncItem(slug)) },
    attempts = 0,
    nextAttemptAt = now,
    lastError = null,
    createdAt = now,
)
