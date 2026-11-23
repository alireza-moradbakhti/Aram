package com.aram.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aram.core.database.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(transactions: List<TransactionEntity>)

    @Query("SELECT * FROM transactions WHERE userId = :userId AND id IN (:ids)")
    suspend fun findByIds(userId: String, ids: List<String>): List<TransactionEntity>

    /**
     * Tombstones rows that the server no longer returns for a fully pulled
     * month. Rows awaiting push or edited after the pull started are spared.
     */
    @Query(
        "UPDATE transactions SET deletedAt = :deletedAt, updatedAt = :deletedAt, pendingPush = 0 " +
            "WHERE userId = :userId AND deletedAt IS NULL AND pendingPush = 0 " +
            "AND occurredAt >= :fromInclusive AND occurredAt < :toExclusive " +
            "AND id NOT IN (:keepIds)",
    )
    suspend fun softDeleteMissing(
        userId: String,
        fromInclusive: Long,
        toExclusive: Long,
        keepIds: List<String>,
        deletedAt: Long,
    ): Int

    @Query(
        "UPDATE transactions SET deletedAt = :deletedAt, updatedAt = :deletedAt, pendingPush = 0 " +
            "WHERE userId = :userId AND deletedAt IS NULL AND pendingPush = 0 " +
            "AND occurredAt >= :fromInclusive AND occurredAt < :toExclusive",
    )
    suspend fun softDeleteAllInRange(
        userId: String,
        fromInclusive: Long,
        toExclusive: Long,
        deletedAt: Long,
    ): Int

    @Query(
        "SELECT * FROM transactions WHERE userId = :userId AND deletedAt IS NULL " +
            "AND occurredAt >= :fromInclusive AND occurredAt < :toExclusive " +
            "ORDER BY occurredAt DESC",
    )
    fun observeRange(
        userId: String,
        fromInclusive: Long,
        toExclusive: Long,
    ): Flow<List<TransactionEntity>>

    @Query(
        "SELECT * FROM transactions WHERE userId = :userId AND deletedAt IS NULL " +
            "ORDER BY occurredAt DESC LIMIT :limit",
    )
    fun observeRecent(userId: String, limit: Int): Flow<List<TransactionEntity>>

    @Query(
        "SELECT * FROM transactions WHERE userId = :userId AND id = :id " +
            "AND deletedAt IS NULL",
    )
    fun observeById(userId: String, id: String): Flow<TransactionEntity?>

    @Query(
        "SELECT * FROM transactions WHERE userId = :userId AND id = :id " +
            "AND deletedAt IS NULL",
    )
    suspend fun findById(userId: String, id: String): TransactionEntity?

    @Query(
        "UPDATE transactions SET accountId = :accountId, categoryId = :categoryId, " +
            "type = :type, amountMinor = :amountMinor, currencyCode = :currencyCode, " +
            "occurredAt = :occurredAt, note = :note, updatedAt = :updatedAt, pendingPush = 1 " +
            "WHERE userId = :userId AND id = :id AND deletedAt IS NULL",
    )
    suspend fun update(
        userId: String,
        id: String,
        accountId: String,
        categoryId: String?,
        type: String,
        amountMinor: Long,
        currencyCode: String,
        occurredAt: Long,
        note: String?,
        updatedAt: Long,
    ): Int

    @Query(
        "UPDATE transactions SET deletedAt = :deletedAt, updatedAt = :deletedAt, pendingPush = 1 " +
            "WHERE userId = :userId AND id = :id AND deletedAt IS NULL",
    )
    suspend fun softDelete(userId: String, id: String, deletedAt: Long): Int

    @Query(
        "SELECT COALESCE(SUM(amountMinor), 0) FROM transactions " +
            "WHERE userId = :userId AND type = :type AND deletedAt IS NULL " +
            "AND occurredAt BETWEEN :fromInclusive AND :toExclusive",
    )
    fun observeTotal(userId: String, type: String, fromInclusive: Long, toExclusive: Long): Flow<Long>

    @Query("DELETE FROM transactions WHERE userId = :userId")
    suspend fun deleteByUser(userId: String)
}
