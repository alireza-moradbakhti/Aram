package com.aram.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "categories",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["userId", "kind"]),
    ],
)
data class CategoryEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val kind: String,
    val parentId: String?,
    val colorArgb: Int,
    val iconKey: String,
    val sortOrder: Int,
    val archived: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
    /** Server catalog slug used by /sync/push; null for local-only rows. */
    val slug: String? = null,
)
