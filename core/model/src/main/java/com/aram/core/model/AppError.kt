package com.aram.core.model

/**
 * Every expected failure the app can show. Kinds mirror the error codes in
 * `docs/API_CONTRACT.md`, so the network layer maps its `ApiError` 1:1.
 *
 * Pure Kotlin on purpose: no Android or JVM types, so this file can move to
 * `commonMain` when the project adopts Kotlin Multiplatform.
 */
sealed interface AppError {

    data class Validation(val fields: Map<String, String> = emptyMap()) : AppError

    data object Unauthorized : AppError

    data object Forbidden : AppError

    data object NotFound : AppError

    data class Conflict(val message: String? = null) : AppError

    data class RateLimited(val retryAfterSeconds: Long? = null) : AppError

    data object Offline : AppError

    data object Timeout : AppError

    data class Server(val requestId: String? = null) : AppError

    data class Storage(val cause: Throwable? = null) : AppError

    data object Permission : AppError

    data class Unknown(val cause: Throwable? = null) : AppError
}
