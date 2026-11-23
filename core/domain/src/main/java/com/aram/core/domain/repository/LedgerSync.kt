package com.aram.core.domain.repository

import com.aram.core.model.SyncState
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow

/**
 * Refreshes the Room cache from the API. Reads stay on Room; this only decides
 * when to pull. Guests (no token) are a no-op, and a failed pull never clears
 * cached rows.
 */
interface LedgerSync {

    /** Status of the last attempt, for banners and pull-to-refresh indicators. */
    val state: Flow<SyncState>

    /**
     * Pulls the ledger for [month]. Skips the network when the same month was
     * synced less than 15 minutes ago unless [force] is set.
     */
    suspend fun refresh(month: YearMonth, force: Boolean = false)

    /**
     * Push-first full sync: drains the outbox, pulls changes since the stored
     * cursor, then refreshes the current month. Returns false when the pull
     * failed so background work can retry.
     */
    suspend fun syncNow(): Boolean
}
