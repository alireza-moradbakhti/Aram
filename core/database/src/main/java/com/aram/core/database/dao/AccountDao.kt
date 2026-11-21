package com.aram.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aram.core.database.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(accounts: List<AccountEntity>)

    @Query(
        "SELECT * FROM accounts WHERE userId = :userId AND deletedAt IS NULL " +
            "ORDER BY sortOrder ASC",
    )
    fun observeByUser(userId: String): Flow<List<AccountEntity>>

    @Query(
        "SELECT * FROM accounts WHERE userId = :userId AND id = :id " +
            "AND deletedAt IS NULL",
    )
    suspend fun findById(userId: String, id: String): AccountEntity?

    @Query("SELECT COUNT(*) FROM accounts WHERE userId = :userId AND deletedAt IS NULL")
    suspend fun countByUser(userId: String): Int

    @Query("DELETE FROM accounts WHERE userId = :userId")
    suspend fun deleteByUser(userId: String)
}
