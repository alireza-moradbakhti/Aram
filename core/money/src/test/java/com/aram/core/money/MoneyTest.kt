package com.aram.core.money

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class MoneyTest {

    @Test
    fun `major units convert to minor units without floating point drift`() {
        assertEquals(1234L, Money.ofMajor("12.34", CurrencyCode.USD).amountMinor)
        assertEquals(1L, Money.ofMajor("0.01", CurrencyCode.USD).amountMinor)
        assertEquals(120000L, Money.ofMajor("120000", CurrencyCode.IRR).amountMinor)
        assertEquals(1L, Money.ofMajor("1", CurrencyCode.JPY).amountMinor)
    }

    @Test
    fun `rounding uses half up at the minor unit boundary`() {
        assertEquals(1L, Money.ofMajor("0.005", CurrencyCode.USD).amountMinor)
    }

    @Test
    fun `addition and subtraction keep the currency`() {
        val lunch = Money.ofMajor("12.50", CurrencyCode.USD)
        val coffee = Money.ofMajor("3.25", CurrencyCode.USD)

        assertEquals(1575L, (lunch + coffee).amountMinor)
        assertEquals(925L, (lunch - coffee).amountMinor)
        assertEquals(CurrencyCode.USD, (lunch + coffee).currency)
    }

    @Test
    fun `combining different currencies is rejected`() {
        val usd = Money.ofMajor("10", CurrencyCode.USD)
        val irr = Money.ofMajor("10", CurrencyCode.IRR)

        assertThrows(IllegalArgumentException::class.java) { usd + irr }
        assertThrows(IllegalArgumentException::class.java) { usd.compareTo(irr) }
    }

    @Test
    fun `comparison respects the minor unit value`() {
        val cheap = Money.ofMajor("9.99", CurrencyCode.USD)
        val expensive = Money.ofMajor("10.00", CurrencyCode.USD)

        assertTrue(cheap < expensive)
    }

    @Test
    fun `formatting respects currency fraction digits`() {
        assertEquals(
            "$1,234.50",
            Money.ofMajor("1234.50", CurrencyCode.USD).formatted(Locale.US),
        )
    }

    @Test
    fun `zero and abs behave as expected`() {
        val money = Money.ofMajor("-4.20", CurrencyCode.USD)

        assertEquals(420L, money.abs().amountMinor)
        assertTrue(Money.zero(CurrencyCode.USD).isZero())
        assertTrue(money.isNegative())
    }
}
