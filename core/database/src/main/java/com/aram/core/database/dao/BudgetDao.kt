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

    @Query(
        "SELECT * FROM budgets WHERE userId = :userId AND deletedAt IS NULL " +
            "AND periodStartEpochDay = :periodStartEpochDay",
    )
    fun observeForPeriod(userId: String, periodStartEpochDay: Long): Flow<List<BudgetEntity>>

    @Query("DELETE FROM budgets WHERE userId = :userId")
    suspend fun deleteByUser(userId: String)
}
