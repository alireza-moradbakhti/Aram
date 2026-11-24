package com.aram.core.model

enum class SyncStatus {
    IDLE,
    REFRESHING,
    OFFLINE,
    STALE,
    FAILED,
}

/**
 * Shared by every cache-first screen: what the last refresh attempt did,
 * regardless of the domain being synced.
 */
data class SyncState(
    val status: SyncStatus = SyncStatus.IDLE,
    val lastSyncedAtMillis: Long? = null,
)
