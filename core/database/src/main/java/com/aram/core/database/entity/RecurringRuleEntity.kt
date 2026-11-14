package com.aram.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recurring_rules",
    indices = [
        Index(value = ["userId", "active"]),
        Index(value = ["nextDueEpochDay"]),
    ],
)
data class RecurringRuleEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val accountId: String,
    val categoryId: String?,
    val type: String,
    val amountMinor: Long,
    val currencyCode: String,
    val frequency: String,
    val intervalCount: Int,
    val startEpochDay: Long,
    val nextDueEpochDay: Long,
    val endEpochDay: Long?,
    val isSubscription: Boolean,
    val note: String?,
    val active: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
)
