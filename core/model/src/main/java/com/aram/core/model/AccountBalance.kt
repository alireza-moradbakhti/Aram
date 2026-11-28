package com.aram.core.model

/**
 * A derived account balance in minor units: the account's opening balance plus
 * its non-deleted ledger rows. Never stored.
 */
data class AccountBalance(
    val accountId: String,
    val currencyCode: String,
    val balanceMinor: Long,
)