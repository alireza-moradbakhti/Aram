package com.aram.core.network

const val X_REQUEST_ID = "X-Request-Id"
const val X_PLATFORM = "X-Platform"
const val X_APP_VERSION = "X-App-Version"
const val IDEMPOTENCY_KEY = "Idempotency-Key"

/** Component name for every network log line; payloads are never logged. */
const val NETWORK_LOG_TAG = "http"
