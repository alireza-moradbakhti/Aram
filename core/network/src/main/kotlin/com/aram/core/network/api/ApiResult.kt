package com.aram.core.network.api

/**
 * The single result type of every API call. Feature code never sees Ktor
 * responses or exception types.
 */
sealed interface ApiResult<out T> {

    data class Success<T>(val data: T) : ApiResult<T>

    data class Failure(val error: ApiError) : ApiResult<Nothing>
}
