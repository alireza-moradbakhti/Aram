package com.aram.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aram.core.database.entity.SyncOutboxEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncOutboxDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: SyncOutboxEntity)

    /** Oldest due entries first; failed entries park themselves far in the future. */
    @Query(
        "SELECT * FROM sync_outbox WHERE userId = :userId AND nextAttemptAt <= :now " +
            "ORDER BY createdAt ASC LIMIT :limit",
    )
    suspend fun due(userId: String, now: Long, limit: Int): List<SyncOutboxEntity>

    @Query("UPDATE sync_outbox SET attempts = :attempts, nextAttemptAt = :nextAttemptAt, lastError = :lastError WHERE id = :id")
    suspend fun markAttempt(id: String, attempts: Int, nextAttemptAt: Long, lastError: String?)

    @Query("DELETE FROM sync_outbox WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT COUNT(*) FROM sync_outbox WHERE userId = :userId")
    suspend fun count(userId: String): Int

    @Query("SELECT COUNT(*) FROM sync_outbox WHERE userId = :userId")
    fun observeCount(userId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM sync_outbox WHERE userId = :userId AND nextAttemptAt <= :now")
    suspend fun countDue(userId: String, now: Long): Int

    @Query("SELECT COUNT(*) FROM sync_outbox WHERE userId = :userId AND attempts >= :attempts")
    suspend fun countFailed(userId: String, attempts: Int): Int

    @Query("SELECT COUNT(*) FROM sync_outbox WHERE userId = :userId AND attempts >= :attempts")
    fun observeFailed(userId: String, attempts: Int): Flow<Int>

    @Query("UPDATE sync_outbox SET attempts = 0, nextAttemptAt = 0 WHERE userId = :userId")
    suspend fun retryAll(userId: String)

    @Query("DELETE FROM sync_outbox WHERE userId = :userId AND entityId = :entityId")
    suspend fun deleteForEntity(userId: String, entityId: String)

    /** The local row is canonical now; stop showing it as waiting to sync. */
    @Query("UPDATE transactions SET pendingPush = 0 WHERE userId = :userId AND id = :entityId")
    suspend fun markSynced(userId: String, entityId: String)

    @Query("DELETE FROM sync_outbox WHERE userId = :userId AND entityId = :entityId")
    suspend fun discard(userId: String, entityId: String)

    @Query("DELETE FROM sync_outbox WHERE userId = :userId")
    suspend fun deleteByUser(userId: String)
}
