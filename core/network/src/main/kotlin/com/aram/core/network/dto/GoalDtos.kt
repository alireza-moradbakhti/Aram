package com.aram.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class GoalDto(
    val id: String,
    val name: String,
    val targetIrr: Double,
    val savedIrr: Double,
    val remainingIrr: Double,
    val percent: Double,
    val targetDate: String? = null,
    val iconKey: String = "goal",
    val isReached: Boolean = false,
    val isArchived: Boolean = false,
)

@Serializable
data class CreateGoalRequest(
    val name: String,
    val targetIrr: Double,
    val savedIrr: Double? = null,
    val targetDate: String? = null,
    val iconKey: String? = null,
)

@Serializable
data class UpdateGoalRequest(
    val name: String? = null,
    val targetIrr: Double? = null,
    val targetDate: String? = null,
    val iconKey: String? = null,
    val isArchived: Boolean? = null,
)

@Serializable
data class ContributeGoalRequest(
    val amount: Double,
    val currency: String? = null,
    val occurredAt: String? = null,
)
