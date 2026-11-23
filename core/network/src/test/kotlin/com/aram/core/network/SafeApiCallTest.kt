package com.aram.core.network

import com.aram.core.network.api.ApiError
import com.aram.core.network.api.ApiResult
import com.aram.core.network.api.RetryPolicy
import com.aram.core.network.api.safeApiCall
import com.aram.core.network.api.safeApiCallBare
import com.aram.core.network.api.safeApiCallNoContent
import com.aram.core.network.config.ApiPaths
import com.aram.core.network.config.EndpointProvider
import com.aram.core.network.dto.HealthCheckDto
import com.aram.core.network.dto.ProfileDto
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

    private val profileJson = """
        {
          "id": "u1", "displayName": "Ava", "firstName": "Ava",
          "email": "demo@aram.app", "avatarInitials": "AV", "homeCurrency": "IRR"
        }
    """.trimIndent()

    private fun successBody(data: String) = """{"isSuccess":true,"message":"","errors":[],"data":$data}"""

    @Test
    fun `success unwraps the data field`() = runTest {
        val engine = MockEngine {
            respond(successBody(profileJson), HttpStatusCode.OK, jsonHeaders())
        }
        val result = safeApiCall<ProfileDto>(clientFor(engine), logger, HttpMethod.Get, meUrl())
        assertEquals("u1", (result as ApiResult.Success).data.id)
    }

    @Test
    fun `isSuccess false becomes a server failure with the api message`() = runTest {
        val engine = MockEngine {
            respond(
                """{"isSuccess":false,"message":"That email is already registered.","errors":[],"data":null}""",
                HttpStatusCode.OK,
                jsonHeaders(),
            )
        }
        val error = (safeApiCall<ProfileDto>(clientFor(engine), logger, HttpMethod.Get, meUrl()) as ApiResult.Failure).error
        assertEquals(ApiError.Server(null, "That email is already registered."), error)
    }

    @Test
    fun `success without data becomes unknown`() = runTest {
        val engine = MockEngine {
            respond("""{"isSuccess":true,"message":"","errors":[],"data":null}""", HttpStatusCode.OK, jsonHeaders())
        }
        val result = safeApiCall<ProfileDto>(clientFor(engine), logger, HttpMethod.Get, meUrl())
        assertTrue((result as ApiResult.Failure).error is ApiError.Unknown)
    }

    @Test
    fun `error statuses map to typed failures`() = runTest {
        suspend fun call(status: HttpStatusCode, body: String = "", headers: io.ktor.http.Headers = jsonHeaders()): ApiError =
            (
                safeApiCall<ProfileDto>(
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
            """{"isSuccess":false,"message":"A budget for this category already exists.","errors":[]}""",
        ) as ApiError.Conflict
        assertEquals("A budget for this category already exists.", conflict.message)

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
            """{"statusCode":400,"message":"Validation failed.","errors":{"email":["Email is required."]}}""",
        ) as ApiError.Validation
        assertEquals("Email is required.", validation.fields["email"])
        assertEquals("Validation failed.", validation.message)

        assertTrue(call(HttpStatusCode.UnprocessableEntity) is ApiError.Validation)

        val server = call(
            HttpStatusCode.InternalServerError,
            """{"isSuccess":false,"message":"Something went wrong on the server."}""",
            headers = headersOf(
                HttpHeaders.ContentType to listOf("application/json"),
                "X-Request-Id" to listOf("req-9"),
            ),
        ) as ApiError.Server
        assertEquals("req-9", server.requestId)
        assertEquals("Something went wrong on the server.", server.message)
    }

    @Test
    fun `malformed success body becomes unknown`() = runTest {
        val (logger, messages) = recordingLogger()
        val engine = MockEngine { respond("not json", HttpStatusCode.OK, jsonHeaders()) }
        val result = safeApiCall<ProfileDto>(clientFor(engine), logger, HttpMethod.Get, meUrl())
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
                respond(successBody(profileJson), HttpStatusCode.OK, jsonHeaders())
            }
        }
        val result = safeApiCall<ProfileDto>(
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
            respond(successBody(profileJson), HttpStatusCode.OK, jsonHeaders())
        }
        val result = safeApiCall<ProfileDto>(
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
        val result = safeApiCall<ProfileDto>(
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
        val result = safeApiCall<ProfileDto>(
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
            safeApiCall<ProfileDto>(clientFor(engine), logger, HttpMethod.Get, meUrl())
        }.exceptionOrNull()
        assertTrue(thrown is CancellationException)
    }

    @Test
    fun `no content responses succeed without parsing`() = runTest {
        val engine = MockEngine { respond("", HttpStatusCode.NoContent) }
        val result = safeApiCallNoContent(
            clientFor(engine), logger, HttpMethod.Delete, endpoints.url(ApiPaths.byId(ApiPaths.TRANSACTIONS, "t1")),
        )
        assertTrue(result is ApiResult.Success)
    }

    @Test
    fun `bare endpoint skips the result envelope`() = runTest {
        val engine = MockEngine {
            respond("""{"status":"Healthy","totalDuration":"00:00:00.01"}""", HttpStatusCode.OK, jsonHeaders())
        }
        val result = safeApiCallBare<HealthCheckDto>(
            clientFor(engine), logger, HttpMethod.Get, endpoints.url(ApiPaths.HEALTH),
        )
        assertEquals("Healthy", (result as ApiResult.Success).data.status)
    }

    @Test
    fun `request id is attached and logs never carry payloads`() = runTest {
        val (logger, messages) = recordingLogger()
        val engine = MockEngine {
            respond(
                """{"statusCode":400,"message":"Validation failed.","errors":{"note":["Too long."]}}""",
                HttpStatusCode.BadRequest,
                jsonHeaders(),
            )
        }
        val result = safeApiCall<ProfileDto>(
            client = clientFor(engine),
            logger = logger,
            method = HttpMethod.Post,
            url = endpoints.url(ApiPaths.TRANSACTIONS),
            requestId = "req-fixed",
            configure = { setBody("""{"note":"secret-note","amountIrr":123456}""") },
        )
        assertTrue(result is ApiResult.Failure)
        assertEquals("req-fixed", engine.requestHistory.single().headers["X-Request-Id"])

        val exchangeLog = messages.single { it.startsWith("POST") }
        assertTrue(exchangeLog.contains("POST /api/transactions -> 400"))
        assertFalse(messages.any { it.contains("secret-note") || it.contains("123456") })
    }
}
