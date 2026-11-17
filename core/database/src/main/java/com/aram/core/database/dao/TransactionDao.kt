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
            "occurredAt = :occurredAt, note = :note, updatedAt = :updatedAt " +
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
        "UPDATE transactions SET deletedAt = :deletedAt, updatedAt = :deletedAt " +
            "WHERE userId = :userId AND id = :id AND deletedAt IS NULL",
    )
    suspend fun softDelete(userId: String, id: String, deletedAt: Long): Int

    @Query(
        "SELECT COALESCE(SUM(amountMinor), 0) FROM transactions " +
            "WHERE userId = :userId AND type = :type AND deletedAt IS NULL " +
            "AND occurredAt BETWEEN :fromInclusive AND :toExclusive",
    )
    fun observeTotal(userId: String, type: String, fromInclusive: Long, toExclusive: Long): Flow<Long>
}
