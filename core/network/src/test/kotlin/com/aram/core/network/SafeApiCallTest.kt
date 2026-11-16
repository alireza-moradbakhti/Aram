package com.aram.core.network

import com.aram.core.network.api.ApiError
import com.aram.core.network.api.ApiResult
import com.aram.core.network.api.RetryPolicy
import com.aram.core.network.api.safeApiCall
import com.aram.core.network.api.safeApiCallNoContent
import com.aram.core.network.config.ApiPaths
import com.aram.core.network.config.EndpointProvider
import com.aram.core.network.dto.UserDto
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SafeApiCallTest {

    private val endpoints = EndpointProvider(testConfig)
    private val logger = recordingLogger().first
    private val fastRetry = RetryPolicy(maxAttempts = 2, minBackoffMs = 1, maxBackoffMs = 2)

    private fun meUrl() = endpoints.url(ApiPaths.ME)

    @Test
    fun `success parses data and meta`() = runTest {
        val engine = MockEngine {
            respond(
                content = """{"data":{"id":"u1","email":"demo@aram.app"},"meta":{"server_time":99}}""",
                status = HttpStatusCode.OK,
                headers = jsonHeaders(),
            )
        }
        val result = safeApiCall<UserDto>(
            clientFor(engine), logger, HttpMethod.Get, meUrl(),
        )
        val success = result as ApiResult.Success
        assertEquals("u1", success.data.id)
        assertEquals(99L, success.meta?.serverTime)
    }

    @Test
    fun `error statuses map to typed failures`() = runTest {
        suspend fun call(status: HttpStatusCode, body: String = "", headers: io.ktor.http.Headers = jsonHeaders()): ApiError =
            (
                safeApiCall<UserDto>(
                    clientFor(MockEngine { respond(body, status, headers) }),
                    logger,
                    HttpMethod.Get,
                    meUrl(),
                ) as ApiResult.Failure
                ).error

        assertEquals(ApiError.Unauthorized, call(HttpStatusCode.Unauthorized))
        assertEquals(ApiError.Forbidden, call(HttpStatusCode.Forbidden))
        assertEquals(ApiError.NotFound, call(HttpStatusCode.NotFound))

        val conflict = call(
            HttpStatusCode.Conflict,
            """{"error":{"code":"conflict","message":"stale","details":{"current":{"id":"t1"}}}}""",
        ) as ApiError.Conflict
        assertEquals("stale", conflict.message)
        assertTrue(conflict.currentRow != null)

        val rateLimited = call(
            HttpStatusCode.TooManyRequests,
            headers = headersOf(
                HttpHeaders.ContentType to listOf("application/json"),
                HttpHeaders.RetryAfter to listOf("12"),
            ),
        ) as ApiError.RateLimited
        assertEquals(12L, rateLimited.retryAfterSeconds)

        val validation = call(
            HttpStatusCode.BadRequest,
            """{"error":{"code":"validation_failed","message":"bad","fields":{"amount_minor":"must be positive"}}}""",
        ) as ApiError.Validation
        assertEquals("must be positive", validation.fields["amount_minor"])

        val unprocessable = call(HttpStatusCode.UnprocessableEntity)
        assertTrue(unprocessable is ApiError.Validation)

        val server = call(
            HttpStatusCode.InternalServerError,
            headers = headersOf(
                HttpHeaders.ContentType to listOf("application/json"),
                "X-Request-Id" to listOf("req-9"),
            ),
        ) as ApiError.Server
        assertEquals("req-9", server.requestId)
    }

    @Test
    fun `malformed success body becomes unknown`() = runTest {
        val (logger, messages) = recordingLogger()
        val engine = MockEngine { respond("not json", HttpStatusCode.OK, jsonHeaders()) }
        val result = safeApiCall<UserDto>(clientFor(engine), logger, HttpMethod.Get, meUrl())
        assertTrue((result as ApiResult.Failure).error is ApiError.Unknown)
        assertTrue(messages.any { it.contains("Invalid response") })
    }

    @Test
    fun `get retries a server error once`() = runTest {
        var calls = 0
        val engine = MockEngine {
            if (calls++ == 0) {
                respond("", HttpStatusCode.InternalServerError)
            } else {
                respond("""{"data":{"id":"u1"}}""", HttpStatusCode.OK, jsonHeaders())
            }
        }
        val result = safeApiCall<UserDto>(
            clientFor(engine), logger, HttpMethod.Get, meUrl(), retryPolicy = fastRetry,
        )
        assertTrue(result is ApiResult.Success)
        assertEquals(2, engine.requestHistory.size)
    }

    @Test
    fun `get retries a dropped connection once`() = runTest {
        var calls = 0
        val engine = MockEngine {
            if (calls++ == 0) throw IOException("connection reset")
            respond("""{"data":{"id":"u1"}}""", HttpStatusCode.OK, jsonHeaders())
        }
        val result = safeApiCall<UserDto>(
            clientFor(engine), logger, HttpMethod.Get, meUrl(), retryPolicy = fastRetry,
        )
        assertTrue(result is ApiResult.Success)
        assertEquals(2, calls)
    }

    @Test
    fun `dropped connections map to offline after the budget`() = runTest {
        var calls = 0
        val engine = MockEngine {
            calls++
            throw IOException("connection reset")
        }
        val result = safeApiCall<UserDto>(
            clientFor(engine), logger, HttpMethod.Get, meUrl(), retryPolicy = fastRetry,
        )
        assertEquals(ApiError.Offline, (result as ApiResult.Failure).error)
        assertEquals(2, calls)
    }

    @Test
    fun `request timeouts map to timeout`() = runTest {
        val engine = MockEngine {
            delay(120_000)
            respond("", HttpStatusCode.OK)
        }
        val result = safeApiCall<UserDto>(
            clientFor(engine), logger, HttpMethod.Get, meUrl(), retryPolicy = RetryPolicy.NONE,
        )
        assertEquals(ApiError.Timeout, (result as ApiResult.Failure).error)
    }

    @Test
    fun `writes are never retried`() = runTest {
        val engine = MockEngine { respond("", HttpStatusCode.InternalServerError) }
        val result = safeApiCallNoContent(
            clientFor(engine), logger, HttpMethod.Post, endpoints.url(ApiPaths.TRANSACTIONS), retryPolicy = fastRetry,
        )
        assertTrue(result is ApiResult.Failure)
        assertEquals(1, engine.requestHistory.size)
    }

    @Test
    fun `cancellation is rethrown untouched`() = runTest {
        val engine = MockEngine { throw CancellationException("cancelled") }
        val thrown = runCatching {
            safeApiCall<UserDto>(clientFor(engine), logger, HttpMethod.Get, meUrl())
        }.exceptionOrNull()
        assertTrue(thrown is CancellationException)
    }

    @Test
    fun `no content responses succeed without parsing`() = runTest {
        val engine = MockEngine { respond("", HttpStatusCode.NoContent) }
        val result = safeApiCallNoContent(
            clientFor(engine), logger, HttpMethod.Delete, endpoints.url(ApiPaths.AUTH_LOGOUT),
        )
        assertTrue(result is ApiResult.Success)
    }

    @Test
    fun `request id is attached and logs never carry payloads`() = runTest {
        val (logger, messages) = recordingLogger()
        val engine = MockEngine { request ->
            respond(
                """{"error":{"code":"validation_failed"}}""",
                HttpStatusCode.BadRequest,
                jsonHeaders(),
            )
        }
        val result = safeApiCall<UserDto>(
            client = clientFor(engine),
            logger = logger,
            method = HttpMethod.Post,
            url = endpoints.url(ApiPaths.TRANSACTIONS),
            requestId = "req-fixed",
            configure = { setBody("""{"note":"secret-note","amount_minor":123456}""") },
        )
        assertTrue(result is ApiResult.Failure)
        assertEquals("req-fixed", engine.requestHistory.single().headers["X-Request-Id"])

        val exchangeLog = messages.single { it.startsWith("POST") }
        assertTrue(exchangeLog.contains("POST /api/v1/transactions -> 400"))
        assertFalse(messages.any { it.contains("secret-note") || it.contains("123456") })
    }
}
