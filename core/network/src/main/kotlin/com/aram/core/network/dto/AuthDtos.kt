package com.aram.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class TokensDto(
    val accessToken: String,
    val accessExpiresAt: Long,
    val refreshToken: String,
    val refreshExpiresAt: Long,
)

@Serializable
data class AuthResponseDto(
    val user: UserDto,
    val tokens: TokensDto,
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val displayName: String,
)

@Serializable
data class RefreshRequest(
    val refreshToken: String,
)

@Serializable
data class PatchMeRequest(
    val displayName: String? = null,
    val defaultCurrency: String? = null,
    val calendarSystem: String? = null,
)
