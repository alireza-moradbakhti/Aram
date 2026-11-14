package com.aram.core.data.mapper

import com.aram.core.database.entity.UserEntity
import com.aram.core.model.CalendarSystem
import com.aram.core.model.UserProfile

internal fun UserEntity.toModel(): UserProfile = UserProfile(
    id = id,
    displayName = displayName,
    defaultCurrency = defaultCurrency,
    calendarSystem = runCatching { CalendarSystem.valueOf(calendarSystem) }
        .getOrDefault(CalendarSystem.GREGORIAN),
    createdAt = createdAt,
)
