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
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

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
            endpoints.url(ApiPaths.ME),
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
            endpoints.url(ApiPaths.CATEGORIES),
        )
        assertNull(engine.requestHistory.single().headers[HttpHeaders.Authorization])
    }

    @Test
    fun `401 on an authenticated request notifies the session and does not retry`() = runTest {
        val tokens = FakeTokens("expired")
        val engine = MockEngine { respond("", HttpStatusCode.Unauthorized) }
        val result = safeApiCallNoContent(
            clientFor(engine, tokens),
            logger,
            HttpMethod.Get,
            endpoints.url(ApiPaths.ME),
        )
        assertEquals(ApiError.Unauthorized, (result as ApiResult.Failure).error)
        assertEquals(1, tokens.unauthorizedCount)
        assertEquals(1, engine.requestHistory.size)
    }

    @Test
    fun `401 on an anonymous request leaves the session alone`() = runTest {
        val tokens = FakeTokens(initial = null)
        val engine = MockEngine { respond("", HttpStatusCode.Unauthorized) }
        val result = safeApiCallNoContent(
            clientFor(engine, tokens),
            logger,
            HttpMethod.Post,
            endpoints.url(ApiPaths.AUTH_LOGIN),
        )
        assertTrue(result is ApiResult.Failure)
        assertEquals(0, tokens.unauthorizedCount)
    }

    @Test
    fun `writes are sent exactly once`() = runTest {
        val engine = MockEngine { respond("", HttpStatusCode.NoContent) }
        val client = clientFor(engine)
        safeApiCallNoContent(client, logger, HttpMethod.Delete, endpoints.url(ApiPaths.byId(ApiPaths.TRANSACTIONS, "t1")))
        safeApiCallNoContent(client, logger, HttpMethod.Patch, endpoints.url(ApiPaths.byId(ApiPaths.BUDGETS, "b1")))
        assertEquals(2, engine.requestHistory.size)
    }
}
