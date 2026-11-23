package com.aram.core.network

import com.aram.core.logging.LogConfig
import com.aram.core.logging.LogLevel
import com.aram.core.logging.LogSink
import com.aram.core.logging.Logger
import com.aram.core.network.config.ApiConfig
import com.aram.core.network.config.ApiEnvironment
import com.aram.core.network.config.ApiMode
import com.aram.core.network.http.AramHttpClient
import com.aram.core.network.http.TokenProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf

internal val testConfig = ApiConfig(
    environment = ApiEnvironment.STAGE,
    mode = ApiMode.REMOTE,
    baseUrl = "https://api.aram.test",
    appVersion = "1.0.0-test",
)

internal fun recordingLogger(): Pair<Logger, MutableList<String>> {
    val messages = mutableListOf<String>()
    val logger = Logger(
        config = LogConfig(minLevel = LogLevel.VERBOSE),
        sinks = listOf(LogSink { messages += it.message }),
    )
    return logger to messages
}

internal fun clientFor(
    engine: HttpClientEngine,
    tokens: TokenProvider = TokenProvider.None,
): HttpClient = AramHttpClient.create(testConfig, engine, tokens)

internal fun jsonHeaders(): Headers =
    headersOf(HttpHeaders.ContentType, "application/json")

internal class FakeTokens(initial: String? = "token-1") : TokenProvider {

    var token: String? = initial
    var unauthorizedCount = 0

    override suspend fun accessToken(): String? = token

    override suspend fun onUnauthorized() {
        unauthorizedCount++
    }
}
