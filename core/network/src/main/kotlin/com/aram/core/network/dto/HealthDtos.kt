package com.aram.core.network.dto

import kotlinx.serialization.Serializable

/** `GET /api/health` is the one endpoint without the `Result<T>` envelope. */
@Serializable
data class HealthCheckDto(
    val status: String,
    val totalDuration: String? = null,
)

@Serializable
data class HealthCheckEntryDto(
    val component: String,
    val status: String,
    val description: String? = null,
    val duration: String? = null,
)
