package com.aram.core.network.api

import com.aram.core.logging.Logger
import com.aram.core.network.NETWORK_LOG_TAG
import com.aram.core.network.X_REQUEST_ID
import com.aram.core.network.dto.DataEnvelope
import com.aram.core.network.dto.ErrorEnvelope
import com.aram.core.network.dto.toApiMeta
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.io.IOException
import java.util.UUID
import kotlin.time.TimeSource

/**
 * Executes one API call and maps everything into [ApiResult]. Reads retry
 * transient failures up to [RetryPolicy.maxAttempts]; writes never retry here.
 *
 * A malformed success body becomes [ApiError.Unknown] and is logged once,
 * without payload contents.
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
    when (val executed = executeWithRetries(client, logger, method, url, requestId, retryPolicy, configure)) {
        is ApiResult.Failure -> return executed
        is ApiResult.Success -> {
            val envelope = try {
                executed.data.body<DataEnvelope<T>>()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                logger.e(NETWORK_LOG_TAG, "Invalid response for $method $url (req=$requestId)", e)
                return ApiResult.Failure(ApiError.Unknown(e))
            }
            return ApiResult.Success(envelope.data, envelope.meta?.toApiMeta())
        }
    }
}

/** For endpoints with a `204` or no parseable body (for example logout). */
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

internal fun redactPath(url: String): String {
    val withoutScheme = url.substringAfter("://", url)
    val fromApi = withoutScheme.indexOf("/api/v1")
    val relative = if (fromApi >= 0) withoutScheme.substring(fromApi) else "/" + withoutScheme.substringAfter('/')
    return relative.substringBefore('?').substringBefore('#')
}

private suspend fun HttpResponse.toApiError(logger: Logger): ApiError {
    val body = runCatching { body<ErrorEnvelope>() }.getOrNull()?.error
    return when {
        status == HttpStatusCode.Unauthorized -> ApiError.Unauthorized
        status == HttpStatusCode.Forbidden -> ApiError.Forbidden
        status == HttpStatusCode.NotFound -> ApiError.NotFound
        status == HttpStatusCode.Conflict -> ApiError.Conflict(
            message = body?.message,
            currentRow = body?.details?.get("current"),
        )
        status == HttpStatusCode.TooManyRequests -> ApiError.RateLimited(
            retryAfterSeconds = headers[HttpHeaders.RetryAfter]?.toLongOrNull(),
        )
        status == HttpStatusCode.BadRequest || status == HttpStatusCode.UnprocessableEntity ->
            ApiError.Validation(body?.fields.orEmpty(), body?.message)
        status.value >= 500 -> {
            val requestId = body?.requestId ?: headers[X_REQUEST_ID]
            logger.w(NETWORK_LOG_TAG, "Server error ${status.value} (req=$requestId)")
            ApiError.Server(requestId, body?.message)
        }
        else -> {
            logger.w(NETWORK_LOG_TAG, "Unexpected status ${status.value}")
            ApiError.Unknown()
        }
    }
}
