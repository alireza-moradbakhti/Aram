package com.aram.core.model

/**
 * Result of an operation that is allowed to fail. Repositories return this
 * instead of throwing, so a failure never escapes to a ViewModel as a raw
 * exception.
 */
sealed interface Outcome<out T> {

    data class Success<T>(val value: T) : Outcome<T>

    data class Failure(val error: AppError) : Outcome<Nothing>
}
