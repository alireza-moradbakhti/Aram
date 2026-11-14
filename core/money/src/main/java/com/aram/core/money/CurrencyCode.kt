package com.aram.core.money

enum class CurrencyCode(val isoCode: String, val fractionDigits: Int) {
    USD("USD", 2),
    EUR("EUR", 2),
    GBP("GBP", 2),
    AED("AED", 2),
    TRY("TRY", 2),
    IRR("IRR", 0),
    JPY("JPY", 0),
    ;

    companion object {
        fun fromIsoCode(code: String): CurrencyCode =
            entries.firstOrNull { it.isoCode.equals(code, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unsupported currency: $code")
    }
}
