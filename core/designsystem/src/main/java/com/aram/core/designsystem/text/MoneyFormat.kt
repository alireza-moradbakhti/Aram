package com.aram.core.designsystem.text

import java.text.NumberFormat
import java.util.Locale

/** IRR has no fraction digits, so every amount renders as a grouped integer. */
fun formatAmount(amount: Long): String =
    NumberFormat.getIntegerInstance(Locale.US).format(amount)

fun formatSignedAmount(amount: Long): String = when {
    amount < 0 -> "\u2212${formatAmount(-amount)}"
    else -> "+${formatAmount(amount)}"
}
