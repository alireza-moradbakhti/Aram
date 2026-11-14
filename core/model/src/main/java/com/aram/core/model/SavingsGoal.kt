package com.aram.core.model

data class SavingsGoal(
    val id: String,
    val name: String,
    val targetMinor: Long,
    val currencyCode: String,
    val deadlineEpochDay: Long?,
    val status: GoalStatus,
)

enum class GoalStatus {
    ACTIVE,
    COMPLETED,
    ARCHIVED,
}
