package com.aram.core.money

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import kotlin.math.abs

data class Money(
    val amountMinor: Long,
    val currency: CurrencyCode,
) : Comparable<Money> {

    operator fun plus(other: Money): Money {
        requireCompatible(other)
        return copy(amountMinor = amountMinor + other.amountMinor)
    }

    operator fun minus(other: Money): Money {
        requireCompatible(other)
        return copy(amountMinor = amountMinor - other.amountMinor)
    }

    operator fun times(factor: Int): Money = copy(amountMinor = amountMinor * factor)

    operator fun unaryMinus(): Money = copy(amountMinor = -amountMinor)

    fun abs(): Money = copy(amountMinor = abs(amountMinor))

    fun isZero(): Boolean = amountMinor == 0L

    fun isNegative(): Boolean = amountMinor < 0L

    fun majorUnits(): BigDecimal = BigDecimal.valueOf(amountMinor, currency.fractionDigits)

    fun formatted(locale: Locale = Locale.getDefault()): String {
        val format = NumberFormat.getCurrencyInstance(locale)
        format.currency = Currency.getInstance(currency.isoCode)
        format.minimumFractionDigits = currency.fractionDigits
        format.maximumFractionDigits = currency.fractionDigits
        return format.format(majorUnits())
    }

    override fun compareTo(other: Money): Int {
        requireCompatible(other)
        return amountMinor.compareTo(other.amountMinor)
    }

    private fun requireCompatible(other: Money) {
        require(currency == other.currency) {
            "Cannot combine $currency with ${other.currency}"
        }
    }

    companion object {
        fun zero(currency: CurrencyCode): Money = Money(0L, currency)

        fun ofMajor(amount: BigDecimal, currency: CurrencyCode): Money {
            val minor = amount
                .movePointRight(currency.fractionDigits)
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact()
            return Money(minor, currency)
        }

        fun ofMajor(amount: String, currency: CurrencyCode): Money =
            ofMajor(BigDecimal(amount), currency)
    }
}
