package com.aram.feature.transactions.presentation

import androidx.compose.ui.graphics.vector.ImageVector
import com.aram.core.designsystem.icon.AramIcons
import com.aram.core.model.TransactionType

internal fun categoryIcon(iconKey: String?): ImageVector = when (iconKey) {
    "wallet" -> AramIcons.wallet
    "food" -> AramIcons.cafe
    "groceries" -> AramIcons.cart
    "transport" -> AramIcons.car
    "home" -> AramIcons.home
    "utilities" -> AramIcons.receipt
    "shopping" -> AramIcons.shoppingBag
    "health" -> AramIcons.shield
    "fun" -> AramIcons.smile
    "subscriptions" -> AramIcons.musicNote
    "salary" -> AramIcons.arrowIn
    "freelance" -> AramIcons.trending
    else -> AramIcons.receipt
}

internal fun TransactionType.label(): String = when (this) {
    TransactionType.INCOME -> "Income"
    TransactionType.EXPENSE -> "Expense"
    TransactionType.TRANSFER -> "Transfer"
}
