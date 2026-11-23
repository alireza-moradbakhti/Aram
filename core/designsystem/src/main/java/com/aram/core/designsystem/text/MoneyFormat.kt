package com.aram.core.designsystem.text

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

/** IRR has no fraction digits, so every amount renders as a grouped integer. */
fun formatAmount(amount: Long): String =
    NumberFormat.getIntegerInstance(Locale.US).format(amount)

fun formatSignedAmount(amount: Long): String = when {
    amount < 0 -> "\u2212${formatAmount(-amount)}"
    else -> "+${formatAmount(amount)}"
}

/** Formats a minor-unit amount with the currency's fraction digits (18.50 USD, 18,000 IRR). */
fun formatMinor(amountMinor: Long, fractionDigits: Int): String {
    val format = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = fractionDigits
        maximumFractionDigits = fractionDigits
        isGroupingUsed = true
    }
    return format.format(BigDecimal.valueOf(amountMinor).movePointLeft(fractionDigits))
}

fun formatSignedMinor(amountMinor: Long, fractionDigits: Int): String = when {
    amountMinor < 0 -> "\u2212${formatMinor(-amountMinor, fractionDigits)}"
    else -> "+${formatMinor(amountMinor, fractionDigits)}"
}
