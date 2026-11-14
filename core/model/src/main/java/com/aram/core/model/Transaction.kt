package com.aram.core.model

data class Transaction(
    val id: String,
    val accountId: String,
    val categoryId: String?,
    val type: TransactionType,
    val amountMinor: Long,
    val currencyCode: String,
    val occurredAt: Long,
    val note: String?,
    val transferGroupId: String?,
    val recurringRuleId: String?,
)

enum class TransactionType {
    INCOME,
    EXPENSE,
    TRANSFER,
}
