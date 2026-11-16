package com.aram.core.network.dto

import com.aram.core.network.api.ApiMeta
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/** `{ "data": …, "meta": … }` — the success envelope from the contract. */
@Serializable
data class DataEnvelope<T>(
    val data: T,
    val meta: ApiMetaDto? = null,
)

@Serializable
data class ApiMetaDto(
    val nextCursor: String? = null,
    val serverTime: Long? = null,
)

fun ApiMetaDto.toApiMeta(): ApiMeta = ApiMeta(nextCursor = nextCursor, serverTime = serverTime)

/** `{ "error": { … } }` — the failure envelope from the contract. */
@Serializable
data class ErrorEnvelope(
    val error: ErrorBody,
)

@Serializable
data class ErrorBody(
    val code: String = "",
    val message: String? = null,
    val fields: Map<String, String>? = null,
    val requestId: String? = null,
    val details: JsonObject? = null,
)
