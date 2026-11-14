package com.aram.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val defaultCurrency: String,
    val calendarSystem: String,
    val createdAt: Long,
    val updatedAt: Long,
)
