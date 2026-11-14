package com.aram.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "budgets",
    indices = [
        Index(
            value = ["userId", "categoryId", "periodStartEpochDay"],
            unique = true,
        ),
    ],
)
data class BudgetEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val categoryId: String,
    val periodStartEpochDay: Long,
    val limitMinor: Long,
    val currencyCode: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
)
