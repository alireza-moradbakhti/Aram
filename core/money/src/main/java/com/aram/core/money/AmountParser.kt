package com.aram.core.money

import java.math.BigDecimal
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Reads user-typed amounts without ever touching Double or Float. Both `.` and
 * `,` are accepted as the decimal separator; thousands separators are not.
 */
object AmountParser {

    fun parse(input: String, currency: CurrencyCode): AmountParseResult {
        val text = input.trim().replace(" ", "").replace("\u00A0", "")
        if (text.isEmpty()) return AmountParseResult.Empty

        val normalized = when {
            text.count { it == '.' || it == ',' } > 1 -> return AmountParseResult.Invalid
            else -> text.replace(',', '.')
        }

        if (normalized.any { !it.isDigit() && it != '.' }) return AmountParseResult.Invalid

        val major = normalized.toBigDecimalOrNull() ?: return AmountParseResult.Invalid
        if (major.signum() < 0) return AmountParseResult.Invalid

        val fraction = major.stripTrailingZeros().scale().coerceAtLeast(0)
        if (fraction > currency.fractionDigits) {
            return AmountParseResult.TooManyFractionDigits(currency.fractionDigits)
        }

        val money = Money.ofMajor(major, currency)
        return when {
            money.amountMinor == 0L -> AmountParseResult.Zero
            money.amountMinor < 0L -> AmountParseResult.Invalid
            else -> AmountParseResult.Valid(money)
        }
    }

    fun decimalSeparator(locale: Locale = Locale.getDefault()): Char =
        DecimalFormatSymbols.getInstance(locale).decimalSeparator

    private fun String.toBigDecimalOrNull(): BigDecimal? =
        runCatching { BigDecimal(this) }.getOrNull()
}
