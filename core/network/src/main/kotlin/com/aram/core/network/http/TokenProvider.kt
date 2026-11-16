package com.aram.core.network.http

/**
 * Supplies bearer tokens to the client. AR-040 implements the real
 * Keystore-backed version; until then [None] keeps every request anonymous.
 *
 * Implementations must make [refresh] safe against concurrent 401s; the
 * interceptor serializes calls, but a token source may still be shared.
 */
interface TokenProvider {

    suspend fun accessToken(): String?

    /** Returns `true` when [accessToken] now yields a fresh token. */
    suspend fun refresh(): Boolean

    data object None : TokenProvider {
        override suspend fun accessToken(): String? = null
        override suspend fun refresh(): Boolean = false
    }
}
