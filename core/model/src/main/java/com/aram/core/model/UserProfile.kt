package com.aram.core.model

data class UserProfile(
    val id: String,
    val displayName: String,
    val defaultCurrency: String,
    val calendarSystem: CalendarSystem,
    val createdAt: Long,
)

enum class CalendarSystem {
    GREGORIAN,
    JALALI,
}
