package com.aram.core.domain.repository

import com.aram.core.model.Outcome
import com.aram.core.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface AuthRepository {

    /** Emits once for the current session, then again whenever it changes. */
    fun observeSession(): Flow<Outcome<UserProfile?>>

    /** Guest mode: a profile that never leaves this device. */
    suspend fun createLocalProfile(displayName: String): Outcome<UserProfile>

    /** Creates a server account and starts the session. */
    suspend fun register(displayName: String, email: String, password: String): Outcome<UserProfile>

    suspend fun signIn(email: String, password: String): Outcome<UserProfile>

    suspend fun updateProfile(displayName: String): Outcome<UserProfile>

    /** Drops the session, its token and the user-scoped cache. */
    suspend fun signOut(): Outcome<Unit>
}
