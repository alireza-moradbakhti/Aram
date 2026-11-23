package com.aram.core.datastore

/** The last successful refresh, the month it covered and the pull cursor. */
data class SyncMetadata(
    val lastSyncedAtMillis: Long,
    val month: String,
    val cursor: String? = null,
)

interface SyncMetadataStore {

    suspend fun read(): SyncMetadata?

    suspend fun write(metadata: SyncMetadata)

    suspend fun clear()
}
