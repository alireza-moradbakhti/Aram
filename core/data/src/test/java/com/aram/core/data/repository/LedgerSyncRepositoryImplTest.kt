package com.aram.core.data.repository

import com.aram.core.common.CoroutineDispatchers
import com.aram.core.data.FakeAccountDao
import com.aram.core.data.FakeCategoryDao
import com.aram.core.data.FakeSessionStore
import com.aram.core.data.FakeSessionTokenStore
import com.aram.core.data.FakeSyncMetadataStore
import com.aram.core.data.FakeTransactionDao
import com.aram.core.data.FakeUserDao
import com.aram.core.data.http.SessionTokenProvider
import com.aram.core.database.entity.TransactionEntity
import com.aram.core.database.entity.UserEntity
import com.aram.core.datastore.SessionToken
import com.aram.core.datastore.SyncMetadata
import com.aram.core.domain.repository.OutboxSnapshot
import com.aram.core.domain.repository.OutboxSync
import com.aram.core.logging.Logger
import com.aram.core.model.CalendarSystem
import com.aram.core.model.SyncStatus
import com.aram.core.model.TransactionType
import com.aram.core.network.config.ApiConfig
import com.aram.core.network.config.ApiEnvironment
import com.aram.core.network.config.ApiMode
import com.aram.core.network.config.EndpointProvider
import com.aram.core.network.dto.SyncPushRequest
import com.aram.core.network.dto.SyncTransactionItem
import com.aram.core.network.http.AramHttpClient
import com.aram.core.network.mock.MockApiEngine
import com.aram.core.network.mock.MockApiStore
import com.aram.core.network.mock.MockScenario
import com.aram.core.network.mock.MockScenarioController
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.util.UUID
import io.ktor.client.engine.mock.MockEngine
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
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
class LedgerSyncRepositoryImplTest {

    private val month = YearMonth.of(2026, 11)

    @Test
    fun `refresh pulls categories, accounts and the month into Room`() = runTest {
        val harness = harness()

        harness.repository.refresh(month)

        assertEquals(10, harness.categories.rows.size)
        assertEquals(3, harness.accounts.rows.size)
        assertTrue(harness.transactions.rows.isNotEmpty())
        assertTrue(harness.transactions.rows.values.all { it.userId == harness.userId })
        assertEquals(month.toString(), harness.metadata.value?.month)
        assertEquals(SyncStatus.IDLE, harness.state().status)
        assertEquals(harness.clock.millis(), harness.metadata.value?.lastSyncedAtMillis)
    }

    @Test
    fun `a month synced within the window is not fetched again`() = runTest {
        val harness = harness()
        harness.repository.refresh(month)

        harness.scenarios.select(MockScenario.OFFLINE)
        harness.repository.refresh(month)

        assertEquals(SyncStatus.IDLE, harness.state().status)
    }

    @Test
    fun `forced refresh bypasses the staleness window`() = runTest {
        val harness = harness()
        harness.repository.refresh(month)

        harness.scenarios.select(MockScenario.OFFLINE)
        harness.repository.refresh(month, force = true)

        assertEquals(SyncStatus.OFFLINE, harness.state().status)
    }

    @Test
    fun `an offline refresh keeps the cache and reports offline`() = runTest {
        val harness = harness()
        harness.transactions.upsert(localRow(harness.userId, "cached"))
        harness.scenarios.select(MockScenario.OFFLINE)

        harness.repository.refresh(month)

        assertEquals(SyncStatus.OFFLINE, harness.state().status)
        assertNull(harness.transactions.rows.getValue("cached").deletedAt)
        assertNull(harness.metadata.value)
    }

    @Test
    fun `local pending rows win over the server copy`() = runTest {
        val harness = harness()
        harness.repository.refresh(month)
        val serverId = harness.transactions.rows.keys.first()
        harness.transactions.rows[serverId] = harness.transactions.rows.getValue(serverId).copy(
            note = "edited locally",
            pendingPush = true,
            updatedAt = harness.clock.millis(),
        )

        harness.repository.refresh(month, force = true)

        val row = harness.transactions.rows.getValue(serverId)
        assertEquals("edited locally", row.note)
        assertTrue(row.pendingPush)
    }

    @Test
    fun `rows missing from a complete sweep are tombstoned`() = runTest {
        val harness = harness()
        harness.transactions.upsert(localRow(harness.userId, "ghost"))

        harness.repository.refresh(month)

        assertNotNull(harness.transactions.rows.getValue("ghost").deletedAt)
    }

    @Test
    fun `pending local rows survive the tombstone sweep`() = runTest {
        val harness = harness()
        harness.transactions.upsert(localRow(harness.userId, "outbox").copy(pendingPush = true))

        harness.repository.refresh(month)

        assertNull(harness.transactions.rows.getValue("outbox").deletedAt)
    }

    @Test
    fun `a guest session does nothing`() = runTest {
        val harness = harness()
        harness.session.clear()
        harness.tokenStore.clear()

        harness.repository.refresh(month)

        assertTrue(harness.transactions.rows.isEmpty())
        assertNull(harness.metadata.value)
        assertEquals(SyncStatus.IDLE, harness.state().status)
    }

    @Test
    fun `a server error reports failed without touching the cache`() = runTest {
        val harness = harness()
        harness.transactions.upsert(localRow(harness.userId, "cached"))
        harness.scenarios.select(MockScenario.SERVER_ERROR)

        harness.repository.refresh(month)

        assertEquals(SyncStatus.FAILED, harness.state().status)
        assertNull(harness.transactions.rows.getValue("cached").deletedAt)
    }

    @Test
    fun `syncNow drains the outbox before pulling`() = runTest {
        val harness = harness()

        assertTrue(harness.repository.syncNow())

        assertEquals(1, harness.outbox.flushes)
        val flushAt = harness.events.indexOf("flush")
        val pullAt = harness.events.indexOf("GET /api/sync/pull")
        assertTrue(flushAt in 0 until pullAt)
    }

    @Test
    fun `a completed sync stores the server time as the cursor`() = runTest {
        val harness = harness()

        harness.repository.refresh(month)

        assertEquals("2026-11-21T10:00:00Z", harness.metadata.value?.cursor)
    }

    @Test
    fun `a failed pull keeps the previous cursor and reports failure`() = runTest {
        val harness = harness()
        val cursor = "2026-11-01T00:00:00Z"
        harness.metadata.value = SyncMetadata(1L, month.toString(), cursor)
        harness.scenarios.select(MockScenario.OFFLINE)

        assertFalse(harness.repository.syncNow())

        assertEquals(cursor, harness.metadata.value?.cursor)
        assertEquals(SyncStatus.OFFLINE, harness.state().status)
    }

    @Test
    fun `delta rows outside the swept month are cached`() = runTest {
        val harness = harness()
        harness.store.syncPush(
            token = "mock.${harness.userId}",
            request = SyncPushRequest(
                items = listOf(
                    SyncTransactionItem(
                        id = "last-month",
                        kind = "expense",
                        amountIrr = 50_000.0,
                        categorySlug = "groceries",
                        occurredAt = "2026-10-15T09:00:00Z",
                        clientUpdatedAt = "2026-11-21T09:00:00Z",
                    ),
                ),
            ),
        )

        harness.repository.refresh(month)

        val row = harness.transactions.rows.getValue("last-month")
        assertNull(row.deletedAt)
    }

    @Test
    fun `a full delta page keeps the old cursor`() = runTest {
        val harness = harness()
        val cursor = "2026-11-01T00:00:00Z"
        harness.metadata.value = SyncMetadata(1L, month.toString(), cursor)
        harness.store.syncPush(
            token = "mock.${harness.userId}",
            request = SyncPushRequest(
                items = (1..500).map { index ->
                    SyncTransactionItem(
                        id = "bulk-$index",
                        kind = "expense",
                        amountIrr = 1_000.0,
                        categorySlug = "groceries",
                        occurredAt = "2026-11-20T09:00:00Z",
                        clientUpdatedAt = "2026-11-20T09:00:00Z",
                    )
                },
            ),
        )

        harness.repository.refresh(month, force = true)

        assertEquals(cursor, harness.metadata.value?.cursor)
    }

    @Test
    fun `a guest sync does not touch the outbox or metadata`() = runTest {
        val harness = harness()
        harness.session.clear()
        harness.tokenStore.clear()

        assertTrue(harness.repository.syncNow())

        assertEquals(0, harness.outbox.flushes)
        assertNull(harness.metadata.value)
    }

    @Test
    fun `two devices converge after one edit and one delete`() = runTest {
        val store = MockApiStore(clock = CLOCK)
        val deviceA = Harness(UnconfinedTestDispatcher(testScheduler), store)
        val deviceB = Harness(UnconfinedTestDispatcher(testScheduler), store)
        deviceA.repository.refresh(month)
        deviceB.repository.refresh(month)
        val ids = deviceA.transactions.rows.keys.toList()
        val editedId = ids[0]
        val deletedId = ids[1]
        val token = "mock.${deviceA.userId}"

        store.syncPush(
            token = token,
            request = SyncPushRequest(
                items = listOf(
                    SyncTransactionItem(
                        id = editedId,
                        kind = "expense",
                        amountIrr = 100_000.0,
                        categorySlug = "groceries",
                        note = "edited on device B",
                        occurredAt = "2026-11-10T09:00:00Z",
                        clientUpdatedAt = "2026-12-01T00:00:00Z",
                    ),
                ),
            ),
        )
        store.deleteTransaction(token, deletedId)

        deviceA.repository.refresh(month, force = true)
        deviceA.repository.refresh(month, force = true)

        assertEquals("edited on device B", deviceA.transactions.rows.getValue(editedId).note)
        assertNotNull(deviceA.transactions.rows.getValue(deletedId).deletedAt)
    }

    private fun TestScope.harness(): Harness = Harness(UnconfinedTestDispatcher(testScheduler))

    private class Harness(
        dispatcher: CoroutineDispatcher,
        val store: MockApiStore = MockApiStore(clock = CLOCK),
    ) {
        val clock: Clock = CLOCK
        val events = mutableListOf<String>()
        val scenarios = MockScenarioController()
        val outbox = RecordingOutboxSync(events)
        val users = FakeUserDao()
        val categories = FakeCategoryDao()
        val accounts = FakeAccountDao()
        val transactions = FakeTransactionDao()
        val session = FakeSessionStore()
        val tokenStore = FakeSessionTokenStore()
        val metadata = FakeSyncMetadataStore()

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

        private val delegate = MockApiEngine.create(
            store = store,
            scenarios = scenarios,
            latencyMillis = { 0 },
        )

        private val client = AramHttpClient.create(
            config = config,
            engine = MockEngine { request ->
                events += "${request.method.value} ${request.url.encodedPath}"
                delegate.execute(request)
            },
            tokenProvider = tokenProvider,
        )

        val repository = LedgerSyncRepositoryImpl(
            client = client,
            endpoints = EndpointProvider(config),
            session = session,
            tokenProvider = tokenProvider,
            transactionDao = transactions,
            accountDao = accounts,
            categoryDao = categories,
            userDao = users,
            syncMetadata = metadata,
            outboxSync = outbox,
            dispatchers = dispatchers,
            logger = Logger(),
            clock = clock,
        )

        init {
            users.rows[userId] = UserEntity(
                id = userId,
                displayName = "Ava Bennett",
                defaultCurrency = "IRR",
                calendarSystem = CalendarSystem.GREGORIAN.name,
                createdAt = 0L,
                updatedAt = 0L,
            )
            kotlinx.coroutines.runBlocking {
                session.setUserId(userId)
                tokenStore.write(SessionToken("mock.$userId", clock.millis() + DAY_MILLIS))
            }
        }

        suspend fun state() = repository.state.first()
    }

    private class RecordingOutboxSync(
        private val events: MutableList<String>,
    ) : OutboxSync {

        override val snapshot = MutableStateFlow(OutboxSnapshot())

        var flushes = 0

        override suspend fun flush(): Boolean {
            flushes++
            events += "flush"
            return true
        }

        override suspend fun retryFailed() = Unit

        override suspend fun discard(entityId: String) = Unit
    }

    private companion object {
        val CLOCK: Clock = Clock.fixed(Instant.parse("2026-11-21T10:00:00Z"), ZoneOffset.UTC)
        const val DAY_MILLIS = 24L * 60L * 60L * 1000L

        fun localRow(userId: String, id: String) = TransactionEntity(
            id = id,
            userId = userId,
            type = TransactionType.EXPENSE.name,
            accountId = "acc",
            categoryId = "cat",
            amountMinor = 1_000L,
            currencyCode = "IRR",
            occurredAt = LocalDate.of(2026, 11, 10)
                .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            note = null,
            transferGroupId = null,
            recurringRuleId = null,
            createdAt = 0L,
            updatedAt = 0L,
            deletedAt = null,
            pendingPush = false,
        )
    }
}
