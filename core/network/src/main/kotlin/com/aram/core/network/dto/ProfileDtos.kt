package com.aram.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class ProfileStatsDto(
    val transactions: Int = 0,
    val budgets: Int = 0,
    val goals: Int = 0,
)

@Serializable
data class ProfileDto(
    val id: String,
    val displayName: String,
    val firstName: String,
    val email: String,
    val avatarInitials: String,
    val homeCurrency: String,
    val secondaryDisplay: Boolean = false,
    val language: String = "en",
    val appearance: String = "system",
    val quietHours: String = "",
    val lastSyncedAt: String? = null,
    val stats: ProfileStatsDto = ProfileStatsDto(),
)
