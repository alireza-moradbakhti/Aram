package com.aram.core.network.http

import com.aram.core.network.AramJson
import com.aram.core.network.config.ApiConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json

/**
 * Builds the single HTTP client for the app. The engine is injected so alpha
 * can run a mock and remote builds an OkHttp client without changing features.
 */
object AramHttpClient {

    const val CONNECT_TIMEOUT_MS = 15_000L
    const val SOCKET_TIMEOUT_MS = 20_000L
    const val REQUEST_TIMEOUT_MS = 30_000L

    fun create(
        config: ApiConfig,
        engine: HttpClientEngine,
        tokenProvider: TokenProvider = TokenProvider.None,
    ): HttpClient = HttpClient(engine) {
        expectSuccess = false

        install(ContentNegotiation) {
            json(AramJson)
        }

        install(HttpTimeout) {
            connectTimeoutMillis = CONNECT_TIMEOUT_MS
            socketTimeoutMillis = SOCKET_TIMEOUT_MS
            requestTimeoutMillis = REQUEST_TIMEOUT_MS
        }

        install(AramClientPlugin) {
            this.tokenProvider = tokenProvider
            this.appVersion = config.appVersion
        }
    }
}
