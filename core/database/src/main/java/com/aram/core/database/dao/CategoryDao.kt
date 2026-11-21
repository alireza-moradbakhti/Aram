package com.aram.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aram.core.database.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(categories: List<CategoryEntity>)

    @Query(
        "SELECT * FROM categories WHERE userId = :userId AND deletedAt IS NULL " +
            "ORDER BY kind ASC, sortOrder ASC",
    )
    fun observeByUser(userId: String): Flow<List<CategoryEntity>>

    @Query(
        "SELECT * FROM categories WHERE userId = :userId AND id = :id " +
            "AND deletedAt IS NULL",
    )
    suspend fun findById(userId: String, id: String): CategoryEntity?

    @Query("SELECT COUNT(*) FROM categories WHERE userId = :userId AND deletedAt IS NULL")
    suspend fun countByUser(userId: String): Int

    @Query("DELETE FROM categories WHERE userId = :userId")
    suspend fun deleteByUser(userId: String)
}
