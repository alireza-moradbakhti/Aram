package com.aram.core.network

import com.aram.core.network.api.ApiError
import com.aram.core.network.api.ApiResult
import com.aram.core.network.api.RetryPolicy
import com.aram.core.network.api.safeApiCall
import com.aram.core.network.api.safeApiCallBare
import com.aram.core.network.config.ApiPaths
import com.aram.core.network.config.EndpointProvider
import com.aram.core.network.dto.AuthResponseDto
import com.aram.core.network.dto.HealthCheckDto
import com.aram.core.network.dto.LoginRequest
import com.aram.core.network.dto.ProfileDto
import com.aram.core.network.dto.RegisterRequest
import com.aram.core.network.dto.SaveTransactionRequest
import com.aram.core.network.dto.SyncPullDto
import com.aram.core.network.dto.SyncPushRequest
import com.aram.core.network.dto.SyncPushResultDto
import com.aram.core.network.dto.SyncTransactionItem
import com.aram.core.network.dto.TransactionDto
import com.aram.core.network.dto.TransactionListDto
import com.aram.core.network.http.TokenProvider
import com.aram.core.network.mock.MockApiEngine
import com.aram.core.network.mock.MockApiStore
import com.aram.core.network.mock.MockDemoAccount
import com.aram.core.network.mock.MockScenario
import com.aram.core.network.mock.MockScenarioController
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class MockApiEngineTest {

    private val clock = Clock.fixed(Instant.parse("2026-11-21T12:00:00Z"), ZoneOffset.UTC)
    private val endpoints = EndpointProvider(testConfig)
    private val logger = recordingLogger().first
    private val noRetry = RetryPolicy.NONE

    private class MutableTokens(initial: String? = null) : TokenProvider {
        var token: String? = initial
        override suspend fun accessToken(): String? = token
        override suspend fun onUnauthorized() {
            token = null
        }
    }

    private fun engineClient(
        store: MockApiStore = MockApiStore(clock = clock),
        scenarios: MockScenarioController = MockScenarioController(),
        tokens: TokenProvider = MutableTokens(),
    ): Pair<HttpClient, MockApiStore> =
        clientFor(MockApiEngine.create(store = store, scenarios = scenarios, latencyMillis = { 0 }), tokens) to store

    private inline fun <reified T> HttpRequestBuilder.jsonBody(value: T) {
        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        setBody(AramJson.encodeToString(value))
    }

    private suspend fun HttpClient.login(email: String, password: String): ApiResult<AuthResponseDto> = safeApiCall(
        this,
        logger,
        HttpMethod.Post,
        endpoints.url(ApiPaths.AUTH_LOGIN),
        configure = { jsonBody(LoginRequest(email, password)) },
        retryPolicy = noRetry,
    )

    @Test
    fun `register and me round-trip through json`() = runTest {
        val tokens = MutableTokens()
        val (client, _) = engineClient(tokens = tokens)

        val registered = safeApiCall<AuthResponseDto>(
            client,
            logger,
            HttpMethod.Post,
            endpoints.url(ApiPaths.AUTH_REGISTER),
            configure = {
                jsonBody(RegisterRequest(email = "tester@aram.app", password = "secret123", displayName = "Tess Ter"))
            },
            retryPolicy = noRetry,
        )
        val auth = (registered as ApiResult.Success).data
        assertEquals("Tess Ter", auth.profile.displayName)
        assertTrue(auth.token.isNotBlank())

        tokens.token = auth.token
        val me = safeApiCall<ProfileDto>(client, logger, HttpMethod.Get, endpoints.url(ApiPaths.ME), retryPolicy = noRetry)
        assertEquals("tester@aram.app", (me as ApiResult.Success).data.email)
    }

    @Test
    fun `demo credentials sign in and a wrong password is unauthorized`() = runTest {
        val (client, _) = engineClient()

        val ok = client.login(MockDemoAccount.EMAIL, MockDemoAccount.PASSWORD)
        assertEquals("Ava Bennett", (ok as ApiResult.Success).data.profile.displayName)

        val bad = client.login(MockDemoAccount.EMAIL, "nope-nope")
        assertEquals(ApiError.Unauthorized, (bad as ApiResult.Failure).error)
    }

    @Test
    fun `register validation errors keep their fields`() = runTest {
        val (client, _) = engineClient()
        val result = safeApiCall<AuthResponseDto>(
            client,
            logger,
            HttpMethod.Post,
            endpoints.url(ApiPaths.AUTH_REGISTER),
            configure = { jsonBody(RegisterRequest(email = "x", password = "abc", displayName = "")) },
            retryPolicy = noRetry,
        )
        val error = (result as ApiResult.Failure).error as ApiError.Validation
        assertTrue(error.fields.containsKey("email"))
        assertTrue(error.fields.containsKey("password"))
    }

    @Test
    fun `transactions create list and delete end to end`() = runTest {
        val tokens = MutableTokens()
        val (client, _) = engineClient(tokens = tokens)
        tokens.token = (client.login(MockDemoAccount.EMAIL, MockDemoAccount.PASSWORD) as ApiResult.Success).data.token

        val created = safeApiCall<TransactionDto>(
            client,
            logger,
            HttpMethod.Post,
            endpoints.url(ApiPaths.TRANSACTIONS),
            configure = {
                jsonBody(
                    SaveTransactionRequest(
                        kind = "expense",
                        amount = 500_000.0,
                        categorySlug = "dining",
                        note = "Engine test",
                        occurredAt = "2026-11-20T10:00:00Z",
                    ),
                )
            },
            retryPolicy = noRetry,
        )
        val tx = (created as ApiResult.Success).data
        assertEquals("dining", tx.category.slug)

        val list = safeApiCall<TransactionListDto>(
            client,
            logger,
            HttpMethod.Get,
            endpoints.url("${ApiPaths.TRANSACTIONS}?year=2026&month=11"),
            retryPolicy = noRetry,
        )
        val page = (list as ApiResult.Success).data
        assertEquals(13, page.totalCount)
        assertTrue(page.groups.any { group -> group.items.any { it.id == tx.id } })

        val deleted = safeApiCall<Boolean>(
            client,
            logger,
            HttpMethod.Delete,
            endpoints.url(ApiPaths.byId(ApiPaths.TRANSACTIONS, tx.id)),
            retryPolicy = noRetry,
        )
        assertEquals(true, (deleted as ApiResult.Success).data)
    }

    @Test
    fun `sync push and pull share state`() = runTest {
        val tokens = MutableTokens()
        val (client, _) = engineClient(tokens = tokens)
        tokens.token = (client.login(MockDemoAccount.EMAIL, MockDemoAccount.PASSWORD) as ApiResult.Success).data.token

        val push = safeApiCall<SyncPushResultDto>(
            client,
            logger,
            HttpMethod.Post,
            endpoints.url(ApiPaths.SYNC_PUSH),
            configure = {
                jsonBody(
                    SyncPushRequest(
                        items = listOf(
                            SyncTransactionItem(
                                id = "offline-42",
                                kind = "expense",
                                amountIrr = 120_000.0,
                                categorySlug = "fun",
                                occurredAt = "2026-11-20T09:00:00Z",
                                clientUpdatedAt = "2026-11-20T09:00:00Z",
                            ),
                        ),
                    ),
                )
            },
            retryPolicy = noRetry,
        )
        assertEquals(listOf("offline-42"), (push as ApiResult.Success).data.applied)

        val pull = safeApiCall<SyncPullDto>(
            client,
            logger,
            HttpMethod.Get,
            endpoints.url(ApiPaths.SYNC_PULL),
            retryPolicy = noRetry,
        )
        val snapshot = (pull as ApiResult.Success).data
        assertTrue(snapshot.transactions.any { it.id == "offline-42" })
        assertTrue(snapshot.budgets.isNotEmpty())
        assertTrue(snapshot.goals.isNotEmpty())
        assertTrue(snapshot.settings != null)
    }

    @Test
    fun `offline scenario maps to offline and resets`() = runTest {
        val scenarios = MockScenarioController()
        val (client, _) = engineClient(scenarios = scenarios)

        scenarios.select(MockScenario.OFFLINE)
        val offline = client.login(MockDemoAccount.EMAIL, MockDemoAccount.PASSWORD)
        assertEquals(ApiError.Offline, (offline as ApiResult.Failure).error)

        scenarios.reset()
        assertTrue(client.login(MockDemoAccount.EMAIL, MockDemoAccount.PASSWORD) is ApiResult.Success)
    }

    @Test
    fun `server error scenario maps to a server failure`() = runTest {
        val scenarios = MockScenarioController(MockScenario.SERVER_ERROR)
        val (client, _) = engineClient(scenarios = scenarios)
        val result = client.login(MockDemoAccount.EMAIL, MockDemoAccount.PASSWORD)
        assertTrue((result as ApiResult.Failure).error is ApiError.Server)
    }

    @Test
    fun `unauthorized scenario fires once then returns to normal`() = runTest {
        val scenarios = MockScenarioController(MockScenario.UNAUTHORIZED)
        val tokens = MutableTokens()
        val (client, _) = engineClient(scenarios = scenarios, tokens = tokens)

        val rejected = client.login(MockDemoAccount.EMAIL, MockDemoAccount.PASSWORD)
        assertEquals(ApiError.Unauthorized, (rejected as ApiResult.Failure).error)
        assertEquals(MockScenario.NORMAL, scenarios.scenario.value)

        val retried = client.login(MockDemoAccount.EMAIL, MockDemoAccount.PASSWORD)
        assertTrue(retried is ApiResult.Success)
    }

    @Test
    fun `health is served without the result envelope`() = runTest {
        val (client, _) = engineClient()
        val result = safeApiCallBare<HealthCheckDto>(
            client,
            logger,
            HttpMethod.Get,
            endpoints.url(ApiPaths.HEALTH),
            retryPolicy = noRetry,
        )
        assertEquals("Healthy", (result as ApiResult.Success).data.status)
    }
}
