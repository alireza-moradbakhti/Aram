package com.aram.core.network.config

/**
 * The only place that knows the `/api` prefix. Callers pass relative paths such
 * as [ApiPaths.AUTH_LOGIN].
 */
class EndpointProvider(private val config: ApiConfig) {

    fun url(path: String): String {
        val suffix = if (path.startsWith('/')) path else "/$path"
        return config.normalizedBaseUrl + API_PREFIX + suffix
    }

    private companion object {
        const val API_PREFIX = "/api"
    }
}

/** Paths from the Aram API; the `/api` prefix is added by [EndpointProvider]. */
object ApiPaths {
    const val AUTH_REGISTER = "/auth/register"
    const val AUTH_LOGIN = "/auth/login"
    const val AUTH_FORGOT_PASSWORD = "/auth/forgot-password"
    const val AUTH_RESET_PASSWORD = "/auth/reset-password"
    const val AUTH_CHANGE_PASSWORD = "/auth/change-password"
    const val ME = "/auth/me"
    const val PROFILE = "/auth/profile"
    const val ACCOUNT = "/auth/account"

    const val PROFILE_STATS = "/profile/stats"
    const val PROFILE_ACCOUNTS = "/profile/accounts"
    const val PROFILE_EXPORT = "/profile/export"

    const val CATEGORIES = "/categories"
    const val TRANSACTIONS = "/transactions"
    const val TRANSACTIONS_SUMMARY = "/transactions/summary"
    const val TRANSACTIONS_INSIGHTS_WEEKLY = "/transactions/insights/weekly"

    const val BUDGETS = "/budgets"
    const val BUDGETS_OVERVIEW = "/budgets/overview"
    const val BUDGETS_CATEGORIES = "/budgets/categories"

    const val GOALS = "/goals"

    const val NOTIFICATIONS = "/notifications"
    const val NOTIFICATIONS_READ_ALL = "/notifications/read-all"

    const val DASHBOARD = "/dashboard"

    const val SETTINGS = "/settings"
    const val SETTINGS_CURRENCIES = "/settings/currencies"
    const val SETTINGS_LOCALES = "/settings/locales"
    const val SETTINGS_CONVERT = "/settings/convert"
    const val RATES = "/rates"

    const val SYNC_PUSH = "/sync/push"
    const val SYNC_PULL = "/sync/pull"

    const val HEALTH = "/health"

    fun byId(collection: String, id: String): String =
        "$collection/${encode(id)}"

    fun notificationRead(id: String): String = "${NOTIFICATIONS}/${encode(id)}/read"

    fun notificationUnread(id: String): String = "${NOTIFICATIONS}/${encode(id)}/unread"

    fun contribution(goalId: String): String = "${GOALS}/${encode(goalId)}/contributions"

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
