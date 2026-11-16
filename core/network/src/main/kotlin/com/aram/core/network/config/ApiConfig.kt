package com.aram.core.network.config

/**
 * Resolved network settings for one build. Constructed from `BuildConfig`, so
 * no host literal ever lives in Kotlin.
 *
 * @param allowCleartext debug-only escape hatch for local servers. Production
 *   never allows it, whatever the flag says.
 */
data class ApiConfig(
    val environment: ApiEnvironment,
    val mode: ApiMode,
    val baseUrl: String,
    val appVersion: String,
    val allowCleartext: Boolean = false,
) {
    val normalizedBaseUrl: String = baseUrl.trim().trimEnd('/')

    init {
        require(normalizedBaseUrl.isNotBlank()) { "API base URL must not be blank." }
        require(!normalizedBaseUrl.endsWith(API_PREFIX)) {
            "API base URL must be the host only; paths are added by EndpointProvider."
        }
        if (environment.isProduction) {
            require(!allowCleartext) { "Production API access never allows cleartext traffic." }
        }
        if (!allowCleartext) {
            require(normalizedBaseUrl.startsWith(HTTPS_PREFIX)) {
                "API base URL must use HTTPS unless a debug override allows cleartext."
            }
        }
    }

    private companion object {
        const val API_PREFIX = "/api/v1"
        const val HTTPS_PREFIX = "https://"
    }
}
