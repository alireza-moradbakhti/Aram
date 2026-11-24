package com.aram.core.network

import kotlinx.serialization.json.Json

/**
 * The Aram API speaks camelCase and follows the ASP.NET Core defaults: unknown
 * keys are ignored, absent nullable values are omitted on encode, and dates are
 * ISO-8601 strings. One instance is shared by the client, the mock and tests.
 */
val AramJson: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    encodeDefaults = false
}
