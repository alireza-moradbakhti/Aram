package com.aram.core.network

import com.aram.core.network.api.ApiError
import com.aram.core.network.api.ApiResult
import com.aram.core.network.api.safeApiCallNoContent
import com.aram.core.network.config.ApiPaths
import com.aram.core.network.config.EndpointProvider
import com.aram.core.network.http.TokenProvider
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

class ClientPluginTest {

    private val endpoints = EndpointProvider(testConfig)
    private val logger = recordingLogger().first

    @Test
    fun `protocol headers ride on every request`() = runTest {
        val engine = MockEngine { respond("", HttpStatusCode.NoContent) }
        safeApiCallNoContent(
            clientFor(engine, FakeTokens("abc")),
            logger,
            HttpMethod.Get,
            endpoints.url(ApiPaths.ACCOUNTS),
        )
        val request = engine.requestHistory.single()
        assertEquals("android", request.headers["X-Platform"])
        assertEquals("1.0.0-test", request.headers["X-App-Version"])
        assertEquals("application/json", request.headers[HttpHeaders.Accept])
        assertEquals("Bearer abc", request.headers[HttpHeaders.Authorization])
        assertNull(request.headers["Idempotency-Key"])
    }

    @Test
    fun `anonymous requests carry no authorization header`() = runTest {
        val engine = MockEngine { respond("", HttpStatusCode.NoContent) }
        safeApiCallNoContent(
            clientFor(engine, TokenProvider.None),
            logger,
            HttpMethod.Get,
            endpoints.url(ApiPaths.ACCOUNTS),
        )
        assertNull(engine.requestHistory.single().headers[HttpHeaders.Authorization])
    }

    @Test
    fun `writes carry a stable idempotency key across the auth retry`() = runTest {
        val tokens = FakeTokens("old")
        val engine = MockEngine { request ->
            if (request.headers[HttpHeaders.Authorization] == "Bearer old") {
                respond("", HttpStatusCode.Unauthorized)
            } else {
                respond("", HttpStatusCode.NoContent)
            }
        }
        val result = safeApiCallNoContent(
            clientFor(engine, tokens),
            logger,
            HttpMethod.Post,
            endpoints.url(ApiPaths.TRANSACTIONS),
        )
        assertTrue(result is ApiResult.Success)
        assertEquals(1, tokens.refreshCount)

        val keys = engine.requestHistory.map { it.headers["Idempotency-Key"] }
        assertEquals(2, keys.size)
        assertEquals(keys[0], keys[1])
        UUID.fromString(keys[0])
        assertEquals("Bearer token-2", engine.requestHistory.last().headers[HttpHeaders.Authorization])
    }

    @Test
    fun `concurrent 401s trigger a single refresh`() = runTest {
        // Both requests race on purpose; either ordering (both see 401, or the
        // second starts after the refresh) must produce one refresh and two
        // successes. A gate here would deadlock when the engine serializes
        // requests, so the assertions are scheduling-agnostic.
        val tokens = FakeTokens("token-1")
        val staleResponses = AtomicInteger()
        val engine = MockEngine { request ->
            if (request.headers[HttpHeaders.Authorization] == "Bearer token-1") {
                staleResponses.incrementAndGet()
                respond("", HttpStatusCode.Unauthorized)
            } else {
                respond("", HttpStatusCode.NoContent)
            }
        }
        val client = clientFor(engine, tokens)
        val url = endpoints.url(ApiPaths.ACCOUNTS)

        coroutineScope {
            val first = async { safeApiCallNoContent(client, logger, HttpMethod.Get, url) }
            val second = async { safeApiCallNoContent(client, logger, HttpMethod.Get, url) }
            assertTrue(first.await() is ApiResult.Success)
            assertTrue(second.await() is ApiResult.Success)
        }

        assertEquals(1, tokens.refreshCount)
        assertTrue(staleResponses.get() in 1..2)
        assertEquals(staleResponses.get() + 2, engine.requestHistory.size)
    }

    @Test
    fun `failed refresh surfaces unauthorized`() = runTest {
        val tokens = FakeTokens("token-1").apply { refreshSucceeds = false }
        val engine = MockEngine { respond("", HttpStatusCode.Unauthorized) }
        val result = safeApiCallNoContent(
            clientFor(engine, tokens),
            logger,
            HttpMethod.Get,
            endpoints.url(ApiPaths.ACCOUNTS),
        )
        assertEquals(ApiError.Unauthorized, (result as ApiResult.Failure).error)
        assertEquals(1, tokens.refreshCount)
        assertEquals(1, engine.requestHistory.size)
    }

    @Test
    fun `delete and patch also get idempotency keys`() = runTest {
        val engine = MockEngine { respond("", HttpStatusCode.NoContent) }
        val client = clientFor(engine)
        safeApiCallNoContent(client, logger, HttpMethod.Delete, endpoints.url(ApiPaths.byId(ApiPaths.TRANSACTIONS, "t1")))
        safeApiCallNoContent(client, logger, HttpMethod.Patch, endpoints.url(ApiPaths.byId(ApiPaths.ACCOUNTS, "a1")))
        assertTrue(engine.requestHistory.all { it.headers["Idempotency-Key"] != null })
    }
}
