package com.aram.core.network.dto

import kotlinx.serialization.Serializable

/** One transaction to upsert; [clientUpdatedAt] decides last-write-wins. */
@Serializable
data class SyncTransactionItem(
    val id: String,
    val kind: String = "expense",
    val amountIrr: Double = 0.0,
    val categorySlug: String = "other",
    val accountId: String? = null,
    val note: String? = null,
    val occurredAt: String? = null,
    val clientUpdatedAt: String? = null,
    val enteredCurrency: String? = null,
    val enteredAmount: Double? = null,
    val fxRate: Double? = null,
)

@Serializable
data class SyncPushRequest(
    val items: List<SyncTransactionItem> = emptyList(),
)

@Serializable
data class SyncPushResultDto(
    val applied: List<String> = emptyList(),
    val skipped: List<String> = emptyList(),
    val serverTime: String,
    val lastSyncedAt: String? = null,
)

@Serializable
data class SyncPullDto(
    val serverTime: String,
    val lastSyncedAt: String? = null,
    val transactions: List<TransactionDto> = emptyList(),
    val budgets: List<BudgetDto> = emptyList(),
    val goals: List<GoalDto> = emptyList(),
    val settings: SettingsDto? = null,
)
