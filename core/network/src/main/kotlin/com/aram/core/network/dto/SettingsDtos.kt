package com.aram.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class CurrencyDto(
    val code: String,
    val decimals: Int,
    val symbol: String,
    val rateToIrr: Double,
    val names: Map<String, String> = emptyMap(),
)

@Serializable
data class LocaleDto(
    val code: String,
    val name: String,
    val nativeName: String,
    val direction: String,
)

@Serializable
data class SettingsDto(
    val displayName: String,
    val email: String,
    val avatarInitials: String,
    val homeCurrency: String,
    val secondaryDisplay: Boolean = false,
    val language: String = "en",
    val appearance: String = "system",
    val quietHours: String = "",
    val version: String = "",
    val privacy: String = "",
    val ratesUpdatedAt: String,
    val currencies: List<CurrencyDto> = emptyList(),
    val locales: List<LocaleDto> = emptyList(),
)

@Serializable
data class UpdateSettingsRequest(
    val displayName: String? = null,
    val homeCurrency: String? = null,
    val secondaryDisplay: Boolean? = null,
    val language: String? = null,
    val appearance: String? = null,
    val quietHours: String? = null,
)

@Serializable
data class ConvertCurrencyRequest(
    val from: String = "IRR",
    val to: String = "USD",
    val amount: Double,
)

@Serializable
data class ConversionDto(
    val from: String,
    val to: String,
    val amount: Double,
    val result: Double,
    val rate: Double,
    val updatedAt: String,
)
