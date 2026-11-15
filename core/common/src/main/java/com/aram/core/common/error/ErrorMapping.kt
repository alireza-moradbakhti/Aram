package com.aram.core.common.error

import com.aram.core.logging.Logger
import com.aram.core.model.AppError
import com.aram.core.model.Outcome
import kotlinx.coroutines.CancellationException

/**
 * Maps a throwable to a typed failure. Platform-specific kinds (network,
 * storage) are added by the modules that can see those exception types.
 */
fun Throwable.toAppError(): AppError = when (this) {
    is IllegalArgumentException -> AppError.Validation()
    is SecurityException -> AppError.Permission
    else -> AppError.Unknown(this)
}

/**
 * Runs [block] and turns any failure into [Outcome.Failure], logging it once
 * under [component]. [CancellationException] is always rethrown so structured
 * concurrency keeps working.
 */
suspend fun <T> safeOutcome(
    component: String,
    operation: String,
    logger: Logger,
    mapError: (Throwable) -> AppError = { it.toAppError() },
    block: suspend () -> T,
): Outcome<T> = try {
    Outcome.Success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Throwable) {
    logger.e(component, "$operation failed", e)
    Outcome.Failure(mapError(e))
}
