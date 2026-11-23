package com.aram.core.domain.repository

import kotlinx.coroutines.flow.Flow

/** Queue health for the pending chip and the settings diagnostic row. */
data class OutboxSnapshot(
    val pending: Int = 0,
    val failed: Int = 0,
)

/**
 * Drains locally queued ledger writes. [flush] processes entries oldest first
 * and stops at the first transient failure; permanent failures park until the
 * user retries or discards them.
 */
interface OutboxSync {

    val snapshot: Flow<OutboxSnapshot>

    /**
     * True when nothing is left to push. A local profile has no token, so its
     * queue is left intact and reported as settled until the user signs in.
     */
    suspend fun flush(): Boolean

    /** Clears terminal failures so the next flush tries them again. */
    suspend fun retryFailed()

    /** Drops the queued mutation for [entityId]. */
    suspend fun discard(entityId: String)
}

/** Hands work to WorkManager without leaking the framework into repositories. */
interface OutboxTrigger {
    fun schedule(delayMillis: Long = 0L)
}
