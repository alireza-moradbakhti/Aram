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
            "ORDER BY occurredAt DESC LIMIT :limit",
    )
    fun observeRecent(userId: String, limit: Int): Flow<List<TransactionEntity>>

    @Query(
        "SELECT COALESCE(SUM(amountMinor), 0) FROM transactions " +
            "WHERE userId = :userId AND type = :type AND deletedAt IS NULL " +
            "AND occurredAt BETWEEN :fromInclusive AND :toExclusive",
    )
    fun observeTotal(userId: String, type: String, fromInclusive: Long, toExclusive: Long): Flow<Long>
}
