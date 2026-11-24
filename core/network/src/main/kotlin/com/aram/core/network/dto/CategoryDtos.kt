package com.aram.core.network.dto

import kotlinx.serialization.Serializable

/**
 * The ten seeded categories. Transactions reference the slug; budgets reference
 * the id.
 */
@Serializable
data class CategoryRefDto(
    val id: String,
    val slug: String,
    val name: String,
    val iconKey: String,
    val tone: String,
    val kind: String,
)
