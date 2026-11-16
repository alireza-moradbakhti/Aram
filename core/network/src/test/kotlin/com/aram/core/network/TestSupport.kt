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
import io.ktor.client.engine.mock.MockEngine
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.coroutines.delay

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
    engine: MockEngine,
    tokens: TokenProvider = TokenProvider.None,
): HttpClient = AramHttpClient.create(testConfig, engine, tokens)

internal fun jsonHeaders(): Headers =
    headersOf(HttpHeaders.ContentType, "application/json")

internal class FakeTokens(initial: String? = "token-1") : TokenProvider {

    var token: String? = initial
    var refreshCount = 0
    var refreshDelayMs = 0L
    var refreshSucceeds = true

    override suspend fun accessToken(): String? = token

    override suspend fun refresh(): Boolean {
        refreshCount++
        if (refreshDelayMs > 0) delay(refreshDelayMs)
        if (!refreshSucceeds) return false
        token = "token-${refreshCount + 1}"
        return true
    }
}
