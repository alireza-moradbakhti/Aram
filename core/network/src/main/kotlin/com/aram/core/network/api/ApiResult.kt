package com.aram.core.network.api

/** Transport-level metadata from the contract envelope; `null` when absent. */
data class ApiMeta(
    val nextCursor: String? = null,
    val serverTime: Long? = null,
)

/**
 * The single result type of every API call. Feature code never sees Ktor
 * responses or exception types.
 */
sealed interface ApiResult<out T> {

    data class Success<T>(val data: T, val meta: ApiMeta? = null) : ApiResult<T>

    data class Failure(val error: ApiError) : ApiResult<Nothing>
}
