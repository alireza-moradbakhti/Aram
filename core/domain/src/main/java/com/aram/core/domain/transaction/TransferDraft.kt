package com.aram.core.domain.transaction

/**
 * A move between two of the user's own accounts. The amount is positive; the
 * repository writes one signed `TRANSFER` leg per account. Passing a category
 * is not possible by construction — transfers never carry one.
 */
data class TransferDraft(
    val fromAccountId: String,
    val toAccountId: String,
    val amountMinor: Long,
    val currencyCode: String,
    val occurredAt: Long,
    val note: String? = null,
)