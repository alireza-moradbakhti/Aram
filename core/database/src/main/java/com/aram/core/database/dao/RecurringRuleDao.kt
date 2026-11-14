package com.aram.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aram.core.database.entity.RecurringRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringRuleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: RecurringRuleEntity)

    @Query(
        "SELECT * FROM recurring_rules WHERE userId = :userId AND active = 1 " +
            "AND deletedAt IS NULL ORDER BY nextDueEpochDay ASC",
    )
    fun observeActive(userId: String): Flow<List<RecurringRuleEntity>>
}
