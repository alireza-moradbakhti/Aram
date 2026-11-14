package com.aram.core.domain.repository

import com.aram.core.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface AuthRepository {

    fun observeSession(): Flow<UserProfile?>

    suspend fun createLocalProfile(displayName: String): UserProfile
}
