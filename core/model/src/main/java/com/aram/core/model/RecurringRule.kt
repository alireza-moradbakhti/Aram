package com.aram.core.model

data class RecurringRule(
    val id: String,
    val accountId: String,
    val categoryId: String?,
    val type: TransactionType,
    val amountMinor: Long,
    val currencyCode: String,
    val frequency: RecurrenceFrequency,
    val intervalCount: Int,
    val nextDueEpochDay: Long,
    val isSubscription: Boolean,
    val active: Boolean,
)

enum class RecurrenceFrequency {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY,
}
