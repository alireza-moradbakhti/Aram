package com.aram.core.data.http

import com.aram.core.datastore.SessionStore
import com.aram.core.datastore.SessionTokenStore
import com.aram.core.network.http.TokenProvider
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Feeds the bearer token to the HTTP client. A rejected token ends the
 * session: the API has no refresh endpoint. Cached rows stay on disk so an
 * expiry never destroys writes that still need to sync.
 */
@Singleton
class SessionTokenProvider @Inject constructor(
    private val tokenStore: SessionTokenStore,
    private val sessionStore: SessionStore,
    private val clock: Clock,
) : TokenProvider {

    override suspend fun accessToken(): String? =
        tokenStore.read()
            ?.takeUnless { it.isExpired(clock.millis()) }
            ?.value

    override suspend fun onUnauthorized() {
        tokenStore.clear()
        sessionStore.clear()
    }
}
