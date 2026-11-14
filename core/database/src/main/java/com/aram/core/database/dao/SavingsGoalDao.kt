package com.aram.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aram.core.database.entity.GoalContributionEntity
import com.aram.core.database.entity.SavingsGoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingsGoalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGoal(goal: SavingsGoalEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertContribution(contribution: GoalContributionEntity)

    @Query(
        "SELECT * FROM savings_goals WHERE userId = :userId AND deletedAt IS NULL " +
            "ORDER BY createdAt ASC",
    )
    fun observeGoals(userId: String): Flow<List<SavingsGoalEntity>>

    @Query(
        "SELECT COALESCE(SUM(amountMinor), 0) FROM goal_contributions " +
            "WHERE goalId = :goalId AND deletedAt IS NULL",
    )
    fun observeContributedTotal(goalId: String): Flow<Long>
}
