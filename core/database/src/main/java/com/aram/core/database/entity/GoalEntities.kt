package com.aram.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "savings_goals",
    indices = [Index(value = ["userId", "status"])],
)
data class SavingsGoalEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val targetMinor: Long,
    val currencyCode: String,
    val deadlineEpochDay: Long?,
    val accountId: String?,
    val colorArgb: Int,
    val iconKey: String,
    val status: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
)

@Entity(
    tableName = "goal_contributions",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["goalId", "contributedAt"]),
    ],
)
data class GoalContributionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val goalId: String,
    val amountMinor: Long,
    val contributedAt: Long,
    val note: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
)
