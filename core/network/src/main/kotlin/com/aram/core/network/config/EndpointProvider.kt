package com.aram.core.network.config

/**
 * The only place that knows the `/api/v1` prefix. Callers pass relative paths
 * such as [ApiPaths.AUTH_LOGIN].
 */
class EndpointProvider(private val config: ApiConfig) {

    fun url(path: String): String {
        val suffix = if (path.startsWith('/')) path else "/$path"
        return config.normalizedBaseUrl + API_PREFIX + suffix
    }

    private companion object {
        const val API_PREFIX = "/api/v1"
    }
}

/** Relative paths from the contract; the version prefix is added by [EndpointProvider]. */
object ApiPaths {
    const val AUTH_REGISTER = "/auth/register"
    const val AUTH_LOGIN = "/auth/login"
    const val AUTH_REFRESH = "/auth/refresh"
    const val AUTH_LOGOUT = "/auth/logout"
    const val ME = "/me"
    const val ACCOUNTS = "/accounts"
    const val CATEGORIES = "/categories"
    const val TRANSACTIONS = "/transactions"
    const val BUDGETS = "/budgets"
    const val GOALS = "/goals"
    const val SYNC = "/sync"
    const val SYNC_PUSH = "/sync/push"

    fun byId(collection: String, id: String): String =
        "$collection/${encode(id)}"

    fun contribution(goalId: String, contributionId: String? = null): String {
        val base = "${GOALS}/${encode(goalId)}/contributions"
        return if (contributionId == null) base else "$base/${encode(contributionId)}"
    }

    private fun encode(value: String): String = buildString {
        for (char in value) {
            if (char.isLetterOrDigit() || char in UNRESERVED) {
                append(char)
            } else {
                for (byte in char.toString().encodeToByteArray()) {
                    append('%')
                    append(HEX[(byte.toInt() shr 4) and 0xF])
                    append(HEX[byte.toInt() and 0xF])
                }
            }
        }
    }

    private const val HEX = "0123456789ABCDEF"
    private const val UNRESERVED = "-._~"
}
