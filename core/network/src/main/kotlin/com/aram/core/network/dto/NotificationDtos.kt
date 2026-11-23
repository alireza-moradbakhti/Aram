package com.aram.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class NotificationDto(
    val id: String,
    val type: String,
    val title: String,
    val body: String,
    val iconTone: String,
    val isRead: Boolean = false,
    val occurredAt: String,
    val relativeTime: String = "",
)

@Serializable
data class NotificationGroupDto(
    val label: String,
    val items: List<NotificationDto> = emptyList(),
)

@Serializable
data class NotificationListDto(
    val scope: String = "all",
    val unreadCount: Int = 0,
    val groups: List<NotificationGroupDto> = emptyList(),
)

@Serializable
data class NotificationReadDto(
    val id: String,
    val isRead: Boolean,
    val unreadCount: Int = 0,
)
