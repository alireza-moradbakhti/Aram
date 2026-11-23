package com.aram.core.datastore

/** The last successful refresh and which ledger month it covered. */
data class SyncMetadata(
    val lastSyncedAtMillis: Long,
    val month: String,
)

interface SyncMetadataStore {

    suspend fun read(): SyncMetadata?

    suspend fun write(metadata: SyncMetadata)

    suspend fun clear()
}
