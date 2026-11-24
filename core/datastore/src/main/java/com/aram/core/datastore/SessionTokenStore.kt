package com.aram.core.datastore

/** The bearer token issued by `POST /api/auth/register` or `/login`. */
data class SessionToken(
    val value: String,
    val expiresAtEpochMillis: Long,
) {
    fun isExpired(nowEpochMillis: Long): Boolean = expiresAtEpochMillis <= nowEpochMillis
}

/**
 * Persists the session token outside DataStore. Alpha keeps it in memory;
 * stage/production builds encrypt it with a key that never leaves the
 * AndroidKeyStore. Implementations must never log the token.
 */
interface SessionTokenStore {

    suspend fun read(): SessionToken?

    suspend fun write(token: SessionToken)

    suspend fun clear()
}
