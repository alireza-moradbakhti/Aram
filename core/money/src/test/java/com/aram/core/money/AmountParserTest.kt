package com.aram.core.money

import org.junit.Assert.assertEquals
import org.junit.Test

class AmountParserTest {

    @Test
    fun `comma decimals parse for two-decimal currencies`() {
        val result = AmountParser.parse("12,50", CurrencyCode.USD)

        assertEquals(1_250L, (result as AmountParseResult.Valid).money.amountMinor)
    }

    @Test
    fun `dot decimals parse for two-decimal currencies`() {
        val result = AmountParser.parse("1234.50", CurrencyCode.EUR)

        assertEquals(123_450L, (result as AmountParseResult.Valid).money.amountMinor)
    }

    @Test
    fun `zero-decimal currencies accept whole numbers`() {
        val irr = AmountParser.parse("120000", CurrencyCode.IRR)
        val jpy = AmountParser.parse("1000", CurrencyCode.JPY)

        assertEquals(120_000L, (irr as AmountParseResult.Valid).money.amountMinor)
        assertEquals(1_000L, (jpy as AmountParseResult.Valid).money.amountMinor)
    }

    @Test
    fun `zero-decimal currencies reject any fraction digits`() {
        assertEquals(
            AmountParseResult.TooManyFractionDigits(0),
            AmountParser.parse("12.5", CurrencyCode.IRR),
        )
        assertEquals(
            AmountParseResult.TooManyFractionDigits(0),
            AmountParser.parse("1,5", CurrencyCode.JPY),
        )
    }

    @Test
    fun `too many fraction digits are rejected for two-decimal currencies`() {
        assertEquals(
            AmountParseResult.TooManyFractionDigits(2),
            AmountParser.parse("12.345", CurrencyCode.USD),
        )
    }

    @Test
    fun `trailing zeros do not count as extra fraction digits`() {
        val result = AmountParser.parse("12.500", CurrencyCode.USD)

        assertEquals(1_250L, (result as AmountParseResult.Valid).money.amountMinor)
    }

    @Test
    fun `empty input is reported as empty`() {
        assertEquals(AmountParseResult.Empty, AmountParser.parse("   ", CurrencyCode.USD))
    }

    @Test
    fun `zero amounts are rejected`() {
        assertEquals(AmountParseResult.Zero, AmountParser.parse("0", CurrencyCode.USD))
        assertEquals(AmountParseResult.Zero, AmountParser.parse("0.00", CurrencyCode.USD))
    }

    @Test
    fun `negative and malformed inputs are rejected`() {
        assertEquals(AmountParseResult.Invalid, AmountParser.parse("-5", CurrencyCode.USD))
        assertEquals(AmountParseResult.Invalid, AmountParser.parse("abc", CurrencyCode.USD))
        assertEquals(AmountParseResult.Invalid, AmountParser.parse("1.2.3", CurrencyCode.USD))
        assertEquals(AmountParseResult.Invalid, AmountParser.parse("1,234.56", CurrencyCode.USD))
    }

    @Test
    fun `amounts that overflow minor units are rejected`() {
        assertEquals(
            AmountParseResult.Invalid,
            AmountParser.parse("999999999999999999999", CurrencyCode.USD),
        )
    }

    @Test
    fun `decimal separator follows the locale`() {
        assertEquals(',', AmountParser.decimalSeparator(java.util.Locale.GERMANY))
        assertEquals('.', AmountParser.decimalSeparator(java.util.Locale.US))
    }
}
