package com.aram.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aram.core.database.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(budget: BudgetEntity)

    /** Includes tombstones so a re-created budget can revive its old row. */
    @Query(
        "SELECT * FROM budgets WHERE userId = :userId AND categoryId = :categoryId " +
            "AND periodStartEpochDay = :periodStartEpochDay",
    )
    suspend fun find(userId: String, categoryId: String, periodStartEpochDay: Long): BudgetEntity?

    @Query(
        "SELECT * FROM budgets WHERE userId = :userId AND deletedAt IS NULL " +
            "AND periodStartEpochDay = :periodStartEpochDay",
    )
    fun observeForPeriod(userId: String, periodStartEpochDay: Long): Flow<List<BudgetEntity>>

    @Query(
        "UPDATE budgets SET deletedAt = :deletedAt, updatedAt = :deletedAt " +
            "WHERE userId = :userId AND id = :id AND deletedAt IS NULL",
    )
    suspend fun softDelete(userId: String, id: String, deletedAt: Long): Int

    @Query("DELETE FROM budgets WHERE userId = :userId")
    suspend fun deleteByUser(userId: String)
}