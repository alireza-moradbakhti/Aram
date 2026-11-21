package com.aram.core.network.http

import com.aram.core.network.X_APP_VERSION
import com.aram.core.network.X_PLATFORM
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode

class AramClientConfig {
    lateinit var tokenProvider: TokenProvider
    var appVersion: String = ""
}

/**
 * Client-wide protocol rules:
 * - `X-Platform`, `X-App-Version` and `Accept` on every request,
 * - `Authorization: Bearer` from [TokenProvider],
 * - a `401` on an authenticated request notifies the session exactly once and
 *   is not retried (the API has no refresh token).
 */
val AramClientPlugin = createClientPlugin("AramClient", ::AramClientConfig) {
    val tokens = pluginConfig.tokenProvider
    val appVersion = pluginConfig.appVersion

    onRequest { request, _ ->
        request.headers.append(X_PLATFORM, "android")
        if (appVersion.isNotBlank()) {
            request.headers.append(X_APP_VERSION, appVersion)
        }
        if (!request.headers.contains(HttpHeaders.Accept)) {
            request.headers.append(HttpHeaders.Accept, "application/json")
        }
        tokens.accessToken()?.let { token ->
            request.headers.append(HttpHeaders.Authorization, "Bearer $token")
        }
    }

    on(Send) { request ->
        val call = proceed(request)
        if (call.response.status == HttpStatusCode.Unauthorized &&
            request.headers[HttpHeaders.Authorization] != null
        ) {
            tokens.onUnauthorized()
        }
        call
    }
}
