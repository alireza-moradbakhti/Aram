package com.aram.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val displayName: String,
    val homeCurrency: String? = null,
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)

@Serializable
data class ForgotPasswordRequest(
    val email: String,
)

@Serializable
data class ForgotPasswordResponseDto(
    val message: String,
    val developmentToken: String? = null,
)

@Serializable
data class ResetPasswordRequest(
    val email: String,
    val token: String,
    val newPassword: String,
)

@Serializable
data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String,
)

@Serializable
data class UpdateProfileRequest(
    val displayName: String? = null,
    val homeCurrency: String? = null,
    val secondaryDisplay: Boolean? = null,
    val language: String? = null,
    val appearance: String? = null,
    val quietHours: String? = null,
)

@Serializable
data class DeleteAccountRequest(
    val confirm: String,
    val password: String? = null,
)

/** Issued by register/login; the JWT is valid for 24 hours and never refreshed. */
@Serializable
data class AuthResponseDto(
    val token: String,
    val expiresAtUtc: String,
    val profile: ProfileDto,
)
