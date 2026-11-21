package com.aram.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class DashboardOwnerDto(
    val displayName: String,
    val firstName: String,
    val initials: String,
    val homeCurrency: String,
    val language: String = "en",
)

@Serializable
data class DashboardMonthDto(
    val year: Int,
    val month: Int,
    val daysInMonth: Int,
    val daysLeft: Int,
)

@Serializable
data class SafeToSpendDto(
    val amountIrr: Double,
    val dailyAllowanceIrr: Double,
    val percentOfBudget: Double,
    val status: String = "onTrack",
    val statusLabel: String = "On track",
)

@Serializable
data class DashboardStatsDto(
    val spentIrr: Double,
    val spentDeltaPercent: Double? = null,
    val savedIrr: Double = 0.0,
    val savedDeltaIrr: Double = 0.0,
)

@Serializable
data class DashboardSyncDto(
    val lastSyncedAt: String? = null,
    val offlineFirst: Boolean = true,
)

@Serializable
data class DashboardDto(
    val owner: DashboardOwnerDto,
    val month: DashboardMonthDto,
    val safeToSpend: SafeToSpendDto,
    val stats: DashboardStatsDto,
    val budgets: List<BudgetItemDto> = emptyList(),
    val recentTransactions: List<TransactionDto> = emptyList(),
    val week: WeeklyInsightDto,
    val unreadNotifications: Int = 0,
    val sync: DashboardSyncDto = DashboardSyncDto(),
)
