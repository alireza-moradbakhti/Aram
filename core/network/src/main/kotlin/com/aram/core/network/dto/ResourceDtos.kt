package com.aram.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: String,
    val email: String? = null,
    val displayName: String? = null,
    val defaultCurrency: String? = null,
    val calendarSystem: String? = null,
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
)

@Serializable
data class AccountDto(
    val id: String,
    val name: String,
    val type: String,
    val currencyCode: String,
    val openingBalanceMinor: Long,
    val colorArgb: Int? = null,
    val iconKey: String? = null,
    val sortOrder: Int? = null,
    val archived: Boolean? = null,
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
    val deletedAt: Long? = null,
)

@Serializable
data class CategoryDto(
    val id: String,
    val name: String,
    val kind: String,
    val parentId: String? = null,
    val colorArgb: Int? = null,
    val iconKey: String? = null,
    val archived: Boolean? = null,
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
    val deletedAt: Long? = null,
)

@Serializable
data class TransactionDto(
    val id: String,
    val accountId: String,
    val categoryId: String? = null,
    val type: String,
    val amountMinor: Long,
    val currencyCode: String,
    val occurredAt: Long,
    val note: String? = null,
    val transferGroupId: String? = null,
    val recurringRuleId: String? = null,
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
    val deletedAt: Long? = null,
)

@Serializable
data class BudgetDto(
    val id: String,
    val categoryId: String,
    val periodStartEpochDay: Long,
    val limitMinor: Long,
    val currencyCode: String,
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
    val deletedAt: Long? = null,
)

@Serializable
data class GoalDto(
    val id: String,
    val name: String,
    val targetMinor: Long,
    val currencyCode: String,
    val deadlineEpochDay: Long? = null,
    val status: String,
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
    val deletedAt: Long? = null,
)

@Serializable
data class GoalContributionDto(
    val id: String,
    val goalId: String,
    val amountMinor: Long,
    val occurredAt: Long,
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
    val deletedAt: Long? = null,
)
