package com.aram.core.database.dao

/** Projection of [AccountDao.observeBalance]. */
data class AccountBalanceRow(
    val accountId: String,
    val currencyCode: String,
    val balanceMinor: Long,
)