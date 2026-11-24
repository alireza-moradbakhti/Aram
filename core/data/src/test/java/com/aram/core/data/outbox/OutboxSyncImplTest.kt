package com.aram.core.data.outbox

import com.aram.core.common.CoroutineDispatchers
import com.aram.core.data.FakeSessionStore
import com.aram.core.data.FakeSessionTokenStore
import com.aram.core.data.FakeSyncOutboxDao
import com.aram.core.data.http.SessionTokenProvider
import com.aram.core.database.entity.SyncOutboxEntity
import com.aram.core.database.entity.TransactionEntity
import com.aram.core.datastore.SessionToken
import com.aram.core.logging.Logger
import com.aram.core.network.AramJson
import com.aram.core.network.IDEMPOTENCY_KEY
import com.aram.core.network.config.ApiConfig
import com.aram.core.network.config.ApiEnvironment
import com.aram.core.network.config.ApiMode
import com.aram.core.network.config.EndpointProvider
import com.aram.core.network.dto.ResultEnvelope
import com.aram.core.network.dto.SyncPushResultDto
import com.aram.core.network.http.AramHttpClient
import com.aram.core.network.mock.MockApiEngine
import com.aram.core.network.mock.MockApiStore
import com.aram.core.network.mock.MockOutcome
import com.aram.core.network.mock.MockScenario
import com.aram.core.network.mock.MockScenarioController
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OutboxSyncImplTest {

    @Test
    fun `flush pushes queued creates oldest first and clears them`() = runTest {
        val harness = harness()
        harness.enqueueCreate("tx-1", createdAt = 1L)
        harness.enqueueCreate("tx-2", createdAt = 2L)

        assertTrue(harness.outbox.flush())

        assertTrue(harness.dao.rows.isEmpty())
        assertEquals(listOf("tx-1", "tx-2"), harness.serverIds().filter { it.startsWith("tx-") })
    }

    @Test
    fun `each push carries the queue entry id as the idempotency key`() = runTest {
        val keys = mutableListOf<String?>()
        val engine = MockEngine { request ->
            keys += request.headers[IDEMPOTENCY_KEY]
            respond(
                AramJson.encodeToString(
                    ResultEnvelope(
                        isSuccess = true,
                        data = SyncPushResultDto(
                            applied = listOf("tx-1"),
                            serverTime = "2026-11-21T10:00:00Z",
                        ),
                    ),
                ),
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val harness = harness(engine)
        val entry = harness.enqueueCreate("tx-1", createdAt = 1L)

        assertTrue(harness.outbox.flush())

        assertEquals(listOf<String?>(entry.id), keys)
        assertTrue(harness.dao.rows.isEmpty())
    }

    @Test
    fun `an offline flush backs off and keeps the entry`() = runTest {
        val harness = harness()
        val entry = harness.enqueueCreate("tx-1", createdAt = 1L)
        harness.scenarios.select(MockScenario.OFFLINE)

        assertFalse(harness.outbox.flush())

        val kept = harness.dao.rows.getValue(entry.id)
        assertEquals(1, kept.attempts)
        assertEquals(harness.millis() + 30_000L, kept.nextAttemptAt)
        assertNotNull(kept.lastError)
    }

    @Test
    fun `a queue entry with a broken payload parks instead of retrying forever`() = runTest {
        val harness = harness()
        harness.dao.insert(
            SyncOutboxEntity(
                id = "broken",
                userId = harness.userId,
                entityId = "tx-broken",
                op = SyncOutboxEntity.OP_CREATE,
                payload = "{not json",
                createdAt = 1L,
            ),
        )

        assertTrue(harness.outbox.flush())

        val parked = harness.dao.rows.getValue("broken")
        assertEquals(6, parked.attempts)
        assertTrue(parked.nextAttemptAt > harness.millis())
    }

    @Test
    fun `a queued delete calls the delete endpoint and clears the entry`() = runTest {
        val harness = harness()
        harness.enqueueCreate("tx-1", createdAt = 1L)
        harness.outbox.flush()
        assertTrue(harness.serverIds().contains("tx-1"))

        harness.dao.insert(
            SyncOutboxEntity(
                id = "delete-1",
                userId = harness.userId,
                entityId = "tx-1",
                op = SyncOutboxEntity.OP_DELETE,
                payload = null,
                createdAt = 2L,
            ),
        )

        assertTrue(harness.outbox.flush())
        assertTrue(harness.dao.rows.isEmpty())
        assertFalse(harness.serverIds().contains("tx-1"))
    }

    @Test
    fun `deleting a row the server already dropped succeeds`() = runTest {
        val harness = harness()
        harness.dao.insert(
            SyncOutboxEntity(
                id = "delete-1",
                userId = harness.userId,
                entityId = "missing-row",
                op = SyncOutboxEntity.OP_DELETE,
                payload = null,
                createdAt = 1L,
            ),
        )

        assertTrue(harness.outbox.flush())
        assertTrue(harness.dao.rows.isEmpty())
    }

    @Test
    fun `a guest session has nothing to flush and leaves the queue alone`() = runTest {
        val harness = harness()
        val entry = harness.enqueueCreate("tx-1", createdAt = 1L)
        harness.session.clear()
        harness.tokenStore.clear()

        assertTrue(harness.outbox.flush())

        assertNotNull(harness.dao.rows[entry.id])
        assertFalse(harness.serverIds().contains("tx-1"))
    }

    @Test
    fun `a local profile without a token settles instead of retrying forever`() = runTest {
        val harness = harness()
        val entry = harness.enqueueCreate("tx-1", createdAt = 1L)
        harness.tokenStore.clear()

        assertTrue(harness.outbox.flush())

        val kept = harness.dao.rows.getValue(entry.id)
        assertEquals(0, kept.attempts)
        assertFalse(harness.serverIds().contains("tx-1"))
    }

    @Test
    fun `retryFailed resets parked entries and discard drops them`() = runTest {
        val harness = harness()
        harness.dao.insert(
            SyncOutboxEntity(
                id = "a",
                userId = harness.userId,
                entityId = "tx-a",
                op = SyncOutboxEntity.OP_CREATE,
                payload = "{bad",
                createdAt = 1L,
            ),
        )
        harness.dao.insert(
            SyncOutboxEntity(
                id = "b",
                userId = harness.userId,
                entityId = "tx-b",
                op = SyncOutboxEntity.OP_CREATE,
                payload = "{bad",
                createdAt = 2L,
            ),
        )
        harness.outbox.flush()

        harness.outbox.retryFailed()
        assertEquals(0, harness.dao.rows.getValue("a").attempts)

        harness.outbox.discard("tx-b")
        assertNull(harness.dao.rows["b"])
        assertNotNull(harness.dao.rows["a"])
    }

    private fun TestScope.harness(engine: HttpClientEngine? = null): Harness =
        Harness(UnconfinedTestDispatcher(testScheduler), engine)

    private class Harness(dispatcher: CoroutineDispatcher, engine: HttpClientEngine?) {
        val clock: Clock = Clock.fixed(Instant.parse("2026-11-21T10:00:00Z"), ZoneOffset.UTC)
        val scenarios = MockScenarioController()
        val session = FakeSessionStore()
        val tokenStore = FakeSessionTokenStore()
        val dao = FakeSyncOutboxDao()

        val store = MockApiStore(clock = clock)
        val userId: String = UUID.nameUUIDFromBytes(
            "user:demo@aram.app:1".toByteArray(Charsets.UTF_8),
        ).toString()

        private val dispatchers = object : CoroutineDispatchers {
            override val io: CoroutineDispatcher = dispatcher
            override val default: CoroutineDispatcher = dispatcher
            override val main: CoroutineDispatcher = dispatcher
        }

        private val config = ApiConfig(
            environment = ApiEnvironment.STAGE,
            mode = ApiMode.MOCK,
            baseUrl = "https://mock.aram.local",
            appVersion = "test",
        )

        private val tokenProvider = SessionTokenProvider(tokenStore, session, clock)

        private val client = AramHttpClient.create(
            config = config,
            engine = engine ?: MockApiEngine.create(
                store = store,
                scenarios = scenarios,
                latencyMillis = { 0 },
            ),
            tokenProvider = tokenProvider,
        )

        val outbox = OutboxSyncImpl(
            client = client,
            endpoints = EndpointProvider(config),
            session = session,
            tokenProvider = tokenProvider,
            outboxDao = dao,
            dispatchers = dispatchers,
            logger = Logger(),
            clock = clock,
        )

        init {
            kotlinx.coroutines.runBlocking {
                session.setUserId(userId)
                tokenStore.write(SessionToken("mock.$userId", clock.millis() + DAY_MILLIS))
            }
        }

        fun millis(): Long = clock.millis()

        suspend fun enqueueCreate(entityId: String, createdAt: Long): SyncOutboxEntity {
            val entity = TransactionEntity(
                id = entityId,
                userId = userId,
                type = "expense",
                accountId = "",
                categoryId = "",
                amountMinor = 120_000L,
                currencyCode = "IRR",
                occurredAt = Instant.parse("2026-11-20T09:00:00Z").toEpochMilli(),
                note = null,
                transferGroupId = null,
                recurringRuleId = null,
                createdAt = createdAt,
                updatedAt = createdAt,
                deletedAt = null,
                pendingPush = true,
            )
            val entry = outboxEntry(userId, entity, SyncOutboxEntity.OP_CREATE, "groceries", createdAt)
            dao.insert(entry)
            return entry
        }

        fun serverIds(): List<String> {
            val data = when (val outcome = store.listTransactions("mock.$userId", 2026, 11, null, 1, 100, null)) {
                is MockOutcome.Ok -> outcome.data
                is MockOutcome.Error -> error("list failed: ${outcome.status}")
            }
            return data.groups.flatMap { group -> group.items.map { it.id } }.sorted()
        }
    }

    private companion object {
        const val DAY_MILLIS = 24L * 60L * 60L * 1000L
    }
}
