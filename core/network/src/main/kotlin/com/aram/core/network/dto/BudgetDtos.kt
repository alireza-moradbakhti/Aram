package com.aram.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class BudgetItemDto(
    val id: String,
    val categoryId: String,
    val categorySlug: String,
    val categoryName: String,
    val iconKey: String,
    val limitIrr: Double,
    val spentIrr: Double,
    val remainingIrr: Double,
    val percentUsed: Double,
    val status: String,
    val statusLabel: String,
    val rollover: Boolean = false,
)

@Serializable
data class BudgetOverviewDto(
    val year: Int,
    val month: Int,
    val totalLimitIrr: Double = 0.0,
    val totalSpentIrr: Double = 0.0,
    val totalRemainingIrr: Double = 0.0,
    val percentUsed: Double = 0.0,
    val status: String = "onTrack",
    val statusLabel: String = "On track",
    val daysLeft: Int = 0,
    val perDayLeftIrr: Double = 0.0,
    val resetsOn: String,
    val categories: List<BudgetItemDto> = emptyList(),
)

@Serializable
data class BudgetDto(
    val id: String,
    val categoryId: String,
    val categorySlug: String,
    val categoryName: String,
    val iconKey: String,
    val monthlyLimitIrr: Double,
    val rollover: Boolean = false,
)

@Serializable
data class SaveBudgetRequest(
    val categoryId: String? = null,
    val categorySlug: String? = null,
    val monthlyLimitIrr: Double,
    val rollover: Boolean = false,
)

@Serializable
data class UpdateBudgetRequest(
    val monthlyLimitIrr: Double,
    val rollover: Boolean = false,
)
