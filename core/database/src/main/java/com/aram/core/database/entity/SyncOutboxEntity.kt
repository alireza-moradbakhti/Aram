package com.aram.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One queued ledger mutation. Rows are written in the same Room transaction as
 * the ledger change they describe, so the queue can never disagree with the
 * cache.
 */
@Entity(
    tableName = "sync_outbox",
    indices = [
        Index(value = ["userId", "nextAttemptAt"]),
        Index(value = ["userId", "entityId"]),
    ],
)
data class SyncOutboxEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val entityId: String,
    val op: String,
    val payload: String?,
    val attempts: Int = 0,
    val nextAttemptAt: Long = 0L,
    val lastError: String? = null,
    val createdAt: Long,
) {
    companion object {
        const val OP_CREATE = "CREATE"
        const val OP_UPDATE = "UPDATE"
        const val OP_DELETE = "DELETE"
    }
}
