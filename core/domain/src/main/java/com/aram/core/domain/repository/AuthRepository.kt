package com.aram.core.domain.repository

import com.aram.core.model.Outcome
import com.aram.core.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface AuthRepository {

    /** Emits once for the current session, then again whenever it changes. */
    fun observeSession(): Flow<Outcome<UserProfile?>>

    suspend fun createLocalProfile(displayName: String): Outcome<UserProfile>
}
