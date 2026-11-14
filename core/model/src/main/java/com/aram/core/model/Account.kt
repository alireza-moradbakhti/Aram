package com.aram.core.model

data class Account(
    val id: String,
    val name: String,
    val type: AccountType,
    val currencyCode: String,
    val openingBalanceMinor: Long,
    val colorArgb: Int,
    val iconKey: String,
    val archived: Boolean,
)

enum class AccountType {
    CASH,
    BANK,
    WALLET,
    SAVINGS,
    INVESTMENT,
}
