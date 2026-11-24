package com.aram.core.network.http

/**
 * Supplies the bearer token to the client. The backend issues a 24-hour JWT
 * with no refresh endpoint, so a rejected token means the session is over.
 *
 * AR-040 implements the persistent, Keystore-backed version; until then [None]
 * keeps every request anonymous.
 */
interface TokenProvider {

    suspend fun accessToken(): String?

    /** Called once when an authenticated request comes back `401`. */
    suspend fun onUnauthorized()

    data object None : TokenProvider {
        override suspend fun accessToken(): String? = null
        override suspend fun onUnauthorized() = Unit
    }
}
