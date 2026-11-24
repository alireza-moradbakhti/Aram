package com.aram.core.money

/**
 * Result of reading an amount typed by the user. Only [Valid] carries money;
 * every other case maps to a UI message.
 */
sealed interface AmountParseResult {

    data class Valid(val money: Money) : AmountParseResult

    data object Empty : AmountParseResult

    data object Zero : AmountParseResult

    data object Invalid : AmountParseResult

    data class TooManyFractionDigits(val max: Int) : AmountParseResult
}
