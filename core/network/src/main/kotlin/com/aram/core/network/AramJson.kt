package com.aram.core.network

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy

/**
 * The contract is snake_case and additive, so unknown keys are ignored and
 * absent values stay absent. One instance is shared by the client and tests.
 */
@OptIn(ExperimentalSerializationApi::class)
val AramJson: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    namingStrategy = JsonNamingStrategy.SnakeCase
    encodeDefaults = true
}
