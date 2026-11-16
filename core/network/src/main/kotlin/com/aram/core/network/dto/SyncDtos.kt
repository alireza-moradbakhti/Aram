package com.aram.core.network.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable
data class SyncEnvelope(
    val data: SyncData,
    val meta: SyncMeta? = null,
)

@Serializable
data class SyncData(
    val changes: SyncChanges,
)

@Serializable
data class SyncChanges(
    val accounts: List<AccountDto> = emptyList(),
    val categories: List<CategoryDto> = emptyList(),
    val transactions: List<TransactionDto> = emptyList(),
    val budgets: List<BudgetDto> = emptyList(),
    val goals: List<GoalDto> = emptyList(),
    val goalContributions: List<GoalContributionDto> = emptyList(),
)

@Serializable
data class SyncMeta(
    val nextSince: Long? = null,
    val nextCursor: String? = null,
    val hasMore: Boolean = false,
    val serverTime: Long? = null,
)

@Serializable
data class SyncPushRequest(
    val mutations: List<MutationDto>,
)

@Serializable
data class MutationDto(
    val idempotencyKey: String,
    val entity: String,
    val op: String,
    val id: String,
    val clientUpdatedAt: Long,
    val payload: JsonObject,
)

@Serializable
data class PushEnvelope(
    val data: PushData,
    val meta: PushMeta? = null,
)

@Serializable
data class PushData(
    val results: List<PushResultDto> = emptyList(),
)

@Serializable
data class PushResultDto(
    val idempotencyKey: String,
    val status: String,
    val row: JsonElement? = null,
    val error: JsonElement? = null,
)

@Serializable
data class PushMeta(
    val serverTime: Long? = null,
)
