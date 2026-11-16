package com.aram.core.network.api

import kotlin.random.Random

/**
 * Retry budget for idempotent reads. Writes are never retried by the client;
 * they travel through the outbox (AR-042).
 */
data class RetryPolicy(
    val maxAttempts: Int = 2,
    val minBackoffMs: Long = 200,
    val maxBackoffMs: Long = 600,
    val random: Random = Random.Default,
) {
    init {
        require(maxAttempts >= 1) { "maxAttempts must be at least 1." }
        require(minBackoffMs in 0..maxBackoffMs) { "Backoff bounds are invalid." }
    }

    internal fun backoffMs(attempt: Int): Long {
        val cap = (minBackoffMs shl attempt).coerceAtMost(maxBackoffMs)
        return random.nextLong(minBackoffMs, cap + 1)
    }

    companion object {
        val DEFAULT = RetryPolicy()
        val NONE = RetryPolicy(maxAttempts = 1)
    }
}
