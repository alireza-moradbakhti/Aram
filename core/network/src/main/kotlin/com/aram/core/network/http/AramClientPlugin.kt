package com.aram.core.network.http

import com.aram.core.network.IDEMPOTENCY_KEY
import com.aram.core.network.X_APP_VERSION
import com.aram.core.network.X_PLATFORM
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.util.AttributeKey
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

class AramClientConfig {
    lateinit var tokenProvider: TokenProvider
    var appVersion: String = ""
}

/**
 * Client-wide protocol rules from `docs/API_CONTRACT.md`:
 * - `X-Platform` / `X-App-Version` / `Accept` on every request,
 * - one `Idempotency-Key` per write (stable across the auth retry),
 * - `Authorization` from [TokenProvider],
 * - on `401`, a single refresh is shared by all in-flight requests, then each
 *   request is retried exactly once.
 */
val AramClientPlugin = createClientPlugin("AramClient", ::AramClientConfig) {
    val tokens = pluginConfig.tokenProvider
    val appVersion = pluginConfig.appVersion
    val auth = AuthCoordinator(tokens)

    onRequest { request, _ ->
        request.headers.append(X_PLATFORM, "android")
        if (appVersion.isNotBlank()) {
            request.headers.append(X_APP_VERSION, appVersion)
        }
        if (!request.headers.contains(HttpHeaders.Accept)) {
            request.headers.append(HttpHeaders.Accept, "application/json")
        }
        if (request.method in WRITE_METHODS && !request.headers.contains(IDEMPOTENCY_KEY)) {
            request.headers.append(IDEMPOTENCY_KEY, UUID.randomUUID().toString())
        }
        // Record the generation the attached token belongs to: a 401 after
        // another request refreshed must retry, not refresh again.
        val generation = auth.generation()
        tokens.accessToken()?.let { token ->
            request.headers.append(HttpHeaders.Authorization, "Bearer $token")
        }
        request.attributes.put(AuthGenerationKey, generation)
    }

    on(Send) { request ->
        val generation = request.attributes.getOrNull(AuthGenerationKey) ?: auth.generation()
        var call = proceed(request)

        if (call.response.status == HttpStatusCode.Unauthorized &&
            request.headers[HttpHeaders.Authorization] != null &&
            auth.refreshIfStale(generation)
        ) {
            request.headers.remove(HttpHeaders.Authorization)
            tokens.accessToken()?.let { token ->
                request.headers.append(HttpHeaders.Authorization, "Bearer $token")
            }
            call = proceed(request)
        }

        call
    }
}

private val WRITE_METHODS = setOf(HttpMethod.Post, HttpMethod.Patch, HttpMethod.Delete, HttpMethod.Put)

private val AuthGenerationKey = AttributeKey<Long>("aram.auth.generation")

/**
 * Serializes refreshes and hands out a generation counter: concurrent 401s that
 * saw the same generation trigger exactly one refresh; the rest just retry.
 */
internal class AuthCoordinator(private val tokens: TokenProvider) {

    private val mutex = Mutex()
    private var generation = 0L

    suspend fun generation(): Long = mutex.withLock { generation }

    suspend fun refreshIfStale(seenGeneration: Long): Boolean = mutex.withLock {
        if (generation != seenGeneration) return@withLock true
        val refreshed = tokens.refresh()
        if (refreshed) generation++
        refreshed
    }
}
