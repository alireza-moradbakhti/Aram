package com.aram.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["userId", "occurredAt"]),
        Index(value = ["accountId"]),
        Index(value = ["categoryId"]),
        Index(value = ["transferGroupId"]),
    ],
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val type: String,
    val accountId: String,
    val categoryId: String?,
    val amountMinor: Long,
    val currencyCode: String,
    val occurredAt: Long,
    val note: String?,
    val transferGroupId: String?,
    val recurringRuleId: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
)
