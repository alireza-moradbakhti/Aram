package com.aram.core.domain.transaction

import com.aram.core.model.TransactionType

/**
 * A transaction as entered by the user. The amount is always positive; the id,
 * timestamps, and tombstone belong to the repository.
 */
data class TransactionDraft(
    val accountId: String,
    val categoryId: String?,
    val type: TransactionType,
    val amountMinor: Long,
    val currencyCode: String,
    val occurredAt: Long,
    val note: String? = null,
)
