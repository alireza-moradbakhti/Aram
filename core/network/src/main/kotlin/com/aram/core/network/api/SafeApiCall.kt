package com.aram.core.network.api

import com.aram.core.logging.Logger
import com.aram.core.network.AramJson
import com.aram.core.network.NETWORK_LOG_TAG
import com.aram.core.network.X_REQUEST_ID
import com.aram.core.network.dto.ResultEnvelope
import com.aram.core.network.dto.ValidationErrorEnvelope
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.serialization.json.JsonElement
import kotlinx.io.IOException
import java.util.UUID
import kotlin.time.TimeSource

/**
 * Executes one API call and unwraps the `Result<T>` envelope. Reads retry
 * transient failures up to [RetryPolicy.maxAttempts]; writes never retry here.
 *
 * A success status with an unparseable body, or `isSuccess = false` without an
 * error status, becomes [ApiError.Unknown] / [ApiError.Server].
 */
suspend inline fun <reified T> safeApiCall(
    client: HttpClient,
    logger: Logger,
    method: HttpMethod,
    url: String,
    requestId: String = newRequestId(),
    retryPolicy: RetryPolicy = RetryPolicy.DEFAULT,
    noinline configure: HttpRequestBuilder.() -> Unit = {},
): ApiResult<T> {
    val executed = executeWithRetries(client, logger, method, url, requestId, retryPolicy, configure)
    if (executed is ApiResult.Failure) return executed
    val response = (executed as ApiResult.Success).data

    val text = try {
        response.bodyAsText()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        logger.e(NETWORK_LOG_TAG, "Unreadable response for $method $url (req=$requestId)", e)
        return ApiResult.Failure(ApiError.Unknown(e))
    }

    val envelope = try {
        AramJson.decodeFromString<ResultEnvelope<T>>(text)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        logger.e(NETWORK_LOG_TAG, "Invalid response for $method $url (req=$requestId)", e)
        return ApiResult.Failure(ApiError.Unknown(e))
    }

    if (!envelope.isSuccess) {
        logger.w(NETWORK_LOG_TAG, "$method ${redactPath(url)} returned isSuccess=false")
        return ApiResult.Failure(ApiError.Server(null, envelope.message.ifBlank { null }))
    }
    val data = envelope.data ?: return ApiResult.Failure(ApiError.Unknown())
    return ApiResult.Success(data)
}

/** For the one bare endpoint (`GET /api/health`). */
suspend inline fun <reified T> safeApiCallBare(
    client: HttpClient,
    logger: Logger,
    method: HttpMethod,
    url: String,
    requestId: String = newRequestId(),
    retryPolicy: RetryPolicy = RetryPolicy.DEFAULT,
    noinline configure: HttpRequestBuilder.() -> Unit = {},
): ApiResult<T> {
    val executed = executeWithRetries(client, logger, method, url, requestId, retryPolicy, configure)
    if (executed is ApiResult.Failure) return executed
    val response = (executed as ApiResult.Success).data
    return try {
        ApiResult.Success(response.body<T>())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        logger.e(NETWORK_LOG_TAG, "Invalid response for $method $url (req=$requestId)", e)
        ApiResult.Failure(ApiError.Unknown(e))
    }
}

/** For endpoints that answer `204` or whose body can be ignored. */
suspend fun safeApiCallNoContent(
    client: HttpClient,
    logger: Logger,
    method: HttpMethod,
    url: String,
    requestId: String = newRequestId(),
    retryPolicy: RetryPolicy = RetryPolicy.DEFAULT,
    configure: HttpRequestBuilder.() -> Unit = {},
): ApiResult<Unit> = when (
    val executed = executeWithRetries(client, logger, method, url, requestId, retryPolicy, configure)
) {
    is ApiResult.Failure -> executed
    is ApiResult.Success -> ApiResult.Success(Unit)
}

fun newRequestId(): String = UUID.randomUUID().toString()

suspend fun executeWithRetries(
    client: HttpClient,
    logger: Logger,
    method: HttpMethod,
    url: String,
    requestId: String,
    retryPolicy: RetryPolicy,
    configure: (HttpRequestBuilder) -> Unit,
): ApiResult<HttpResponse> {
    val attempts = if (method == HttpMethod.Get) retryPolicy.maxAttempts else 1
    var lastError: ApiError = ApiError.Unknown()

    repeat(attempts) { attempt ->
        val mark = TimeSource.Monotonic.markNow()
        val outcome: ApiResult<HttpResponse> = try {
            val response = client.request(url) {
                this.method = method
                header(X_REQUEST_ID, requestId)
                configure(this)
            }
            logger.d(
                NETWORK_LOG_TAG,
                "$method ${redactPath(url)} -> ${response.status.value} " +
                    "in ${mark.elapsedNow().inWholeMilliseconds}ms (req=$requestId)",
            )
            if (response.status.isSuccess()) {
                ApiResult.Success(response)
            } else {
                ApiResult.Failure(response.toApiError(logger))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpRequestTimeoutException) {
            ApiResult.Failure(ApiError.Timeout)
        } catch (e: ConnectTimeoutException) {
            ApiResult.Failure(ApiError.Timeout)
        } catch (e: IOException) {
            ApiResult.Failure(ApiError.Offline)
        } catch (e: Throwable) {
            logger.e(NETWORK_LOG_TAG, "$method ${redactPath(url)} failed (req=$requestId)", e)
            ApiResult.Failure(ApiError.Unknown(e))
        }

        if (outcome is ApiResult.Failure && outcome.error.isRetryable() && attempt < attempts - 1) {
            lastError = outcome.error
            delay(retryPolicy.backoffMs(attempt))
        } else {
            return outcome
        }
    }

    return ApiResult.Failure(lastError)
}

internal fun ApiError.isRetryable(): Boolean = when (this) {
    ApiError.Offline, ApiError.Timeout -> true
    is ApiError.Server -> true
    else -> false
}

fun redactPath(url: String): String {
    val withoutScheme = url.substringAfter("://", url)
    val fromApi = withoutScheme.indexOf("/api")
    val relative = if (fromApi >= 0) withoutScheme.substring(fromApi) else "/" + withoutScheme.substringAfter('/')
    return relative.substringBefore('?').substringBefore('#')
}

private suspend fun HttpResponse.toApiError(logger: Logger): ApiError {
    val text = runCatching { bodyAsText() }.getOrDefault("")
    val validation = runCatching {
        AramJson.decodeFromString<ValidationErrorEnvelope>(text)
    }.getOrNull()
    val result = runCatching {
        AramJson.decodeFromString<ResultEnvelope<JsonElement>>(text)
    }.getOrNull()

    val message = validation?.message?.takeIf { it.isNotBlank() }
        ?: result?.message?.takeIf { it.isNotBlank() }
        ?: result?.errors?.firstOrNull()
        ?: validation?.title?.takeIf { it.isNotBlank() }

    return when {
        status == HttpStatusCode.Unauthorized -> ApiError.Unauthorized
        status == HttpStatusCode.Forbidden -> ApiError.Forbidden
        status == HttpStatusCode.NotFound -> ApiError.NotFound
        status == HttpStatusCode.Conflict -> ApiError.Conflict(message)
        status == HttpStatusCode.TooManyRequests -> ApiError.RateLimited(
            retryAfterSeconds = headers[HttpHeaders.RetryAfter]?.toLongOrNull(),
        )
        status == HttpStatusCode.BadRequest || status == HttpStatusCode.UnprocessableEntity -> {
            val fields = validation?.errors.orEmpty()
                .mapValues { (_, messages) -> messages.firstOrNull().orEmpty() }
            ApiError.Validation(fields, message)
        }
        status.value >= 500 -> {
            val requestId = headers[X_REQUEST_ID]
            logger.w(NETWORK_LOG_TAG, "Server error ${status.value} (req=$requestId)")
            ApiError.Server(requestId, message)
        }
        else -> {
            logger.w(NETWORK_LOG_TAG, "Unexpected status ${status.value}")
            ApiError.Unknown()
        }
    }
}
