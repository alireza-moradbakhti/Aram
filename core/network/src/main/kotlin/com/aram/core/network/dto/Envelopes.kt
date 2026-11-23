package com.aram.core.network.dto

import kotlinx.serialization.Serializable

/** `Result<T>` from the API: every JSON endpoint wraps its payload this way. */
@Serializable
data class ResultEnvelope<T>(
    val isSuccess: Boolean = false,
    val message: String = "",
    val errors: List<String> = emptyList(),
    val data: T? = null,
)

/** FastEndpoints' default `400` body; the client maps it to field errors. */
@Serializable
data class ValidationErrorEnvelope(
    val statusCode: Int? = null,
    val message: String? = null,
    val errors: Map<String, List<String>>? = null,
    val title: String? = null,
    val detail: String? = null,
)
