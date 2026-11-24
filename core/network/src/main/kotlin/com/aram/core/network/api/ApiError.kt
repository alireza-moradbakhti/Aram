package com.aram.core.network.api

import com.aram.core.model.AppError

/**
 * Every expected transport/API failure, mapped from the HTTP status and the
 * two error bodies the API produces (`Result` for business errors,
 * FastEndpoints' validation body for `400`).
 */
sealed interface ApiError {

    data object Offline : ApiError

    data object Timeout : ApiError

    data object Unauthorized : ApiError

    data object Forbidden : ApiError

    data object NotFound : ApiError

    data class Validation(
        val fields: Map<String, String> = emptyMap(),
        val message: String? = null,
    ) : ApiError

    data class Conflict(val message: String? = null) : ApiError

    data class RateLimited(val retryAfterSeconds: Long? = null) : ApiError

    data class Server(
        val requestId: String? = null,
        val message: String? = null,
    ) : ApiError

    data class Unknown(val cause: Throwable? = null) : ApiError
}

fun ApiError.toAppError(): AppError = when (this) {
    ApiError.Offline -> AppError.Offline
    ApiError.Timeout -> AppError.Timeout
    ApiError.Unauthorized -> AppError.Unauthorized
    ApiError.Forbidden -> AppError.Forbidden
    ApiError.NotFound -> AppError.NotFound
    is ApiError.Validation -> AppError.Validation(fields)
    is ApiError.Conflict -> AppError.Conflict(message)
    is ApiError.RateLimited -> AppError.RateLimited(retryAfterSeconds)
    is ApiError.Server -> AppError.Server(requestId)
    is ApiError.Unknown -> AppError.Unknown(cause)
}
