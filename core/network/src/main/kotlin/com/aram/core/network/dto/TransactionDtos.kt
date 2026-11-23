package com.aram.core.network.dto

import kotlinx.serialization.Serializable

/**
 * Ledger row. Amounts are decimal IRR; [enteredCurrency]/[enteredAmount] keep
 * what the user typed when it was not IRR.
 */
@Serializable
data class TransactionDto(
    val id: String,
    val kind: String,
    val amountIrr: Double,
    val category: CategoryRefDto,
    val account: AccountRefDto? = null,
    val note: String? = null,
    val occurredAt: String,
    val enteredCurrency: String = "IRR",
    val enteredAmount: Double? = null,
    val fxRate: Double? = null,
)

@Serializable
data class TransactionDayGroupDto(
    val date: String,
    val label: String,
    val totalIrr: Double,
    val items: List<TransactionDto> = emptyList(),
)

/** One UTC month of the ledger, newest day first. */
@Serializable
data class TransactionListDto(
    val year: Int,
    val month: Int,
    val page: Int = 1,
    val pageSize: Int = 50,
    val totalCount: Int = 0,
    val totalPages: Int = 0,
    val totalSpentIrr: Double = 0.0,
    val totalIncomeIrr: Double = 0.0,
    val spentDeltaPercent: Double? = null,
    val groups: List<TransactionDayGroupDto> = emptyList(),
)

@Serializable
data class TransactionSummaryDto(
    val year: Int,
    val month: Int,
    val spentIrr: Double = 0.0,
    val incomeIrr: Double = 0.0,
    val netIrr: Double = 0.0,
    val spentDeltaPercent: Double? = null,
    val transactionCount: Int = 0,
    val daysInMonth: Int = 0,
    val daysLeft: Int = 0,
    val dailyAllowanceIrr: Double = 0.0,
)

@Serializable
data class WeekDayDto(
    val date: String,
    val label: String,
    val amountIrr: Double,
    val isToday: Boolean = false,
)

@Serializable
data class WeeklyInsightDto(
    val weekStart: String,
    val spentIrr: Double = 0.0,
    val deltaPercent: Double? = null,
    val days: List<WeekDayDto> = emptyList(),
)

/**
 * `amount` is expressed in `currency` (the profile home currency when omitted);
 * the server stores IRR. [occurredAt] / [clientUpdatedAt] are ISO-8601 UTC.
 */
@Serializable
data class SaveTransactionRequest(
    val kind: String,
    val amount: Double,
    val currency: String? = null,
    val categorySlug: String? = null,
    val accountId: String? = null,
    val note: String? = null,
    val occurredAt: String? = null,
    val clientUpdatedAt: String? = null,
)
