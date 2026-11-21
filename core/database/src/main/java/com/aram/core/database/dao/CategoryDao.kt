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

    /** Drops local-only categories (e.g. the offline seed) once the server list is in. */
    @Query(
        "UPDATE categories SET deletedAt = :deletedAt, updatedAt = :deletedAt " +
            "WHERE userId = :userId AND deletedAt IS NULL AND id NOT IN (:keepIds) " +
            "AND NOT EXISTS (SELECT 1 FROM transactions t " +
            "WHERE t.categoryId = categories.id AND t.deletedAt IS NULL)",
    )
    suspend fun softDeleteOrphans(userId: String, keepIds: List<String>, deletedAt: Long): Int

    @Query("DELETE FROM categories WHERE userId = :userId")
    suspend fun deleteByUser(userId: String)
}
