package com.aram.core.network.dto

import kotlinx.serialization.Serializable

/** Compact reference embedded in transactions. */
@Serializable
data class AccountRefDto(
    val id: String,
    val name: String,
    val institution: String,
    val kind: String,
)

/** Full account row from `GET /api/profile/accounts`. */
@Serializable
data class AccountDto(
    val id: String,
    val name: String,
    val institution: String,
    val kind: String,
    val isDefault: Boolean = false,
)
