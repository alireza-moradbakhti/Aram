package com.aram.feature.settings.presentation

import com.aram.core.domain.repository.LedgerSync
import com.aram.core.domain.repository.OutboxSnapshot
import com.aram.core.domain.repository.OutboxSync
import com.aram.core.model.SyncState
import com.aram.core.model.SyncStatus
import java.time.Clock
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val clock: Clock = Clock.fixed(Instant.parse("2026-11-21T10:00:00Z"), ZoneOffset.UTC)
    private val ledger = FakeLedgerSync()
    private val outbox = FakeOutboxSync()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `sync status combines the ledger and the outbox`() = runTest(dispatcher) {
        val now = clock.millis()
        ledger.state.value = SyncState(SyncStatus.OFFLINE, now - 5 * 60_000L)
        outbox.snapshot.value = OutboxSnapshot(pending = 2, failed = 1)
        val viewModel = SettingsViewModel(ledger, outbox, clock)
        val job = backgroundScope.launch { viewModel.syncStatus.collect {} }
        advanceUntilIdle()

        val ui = viewModel.syncStatus.value
        assertEquals(SyncStatus.OFFLINE, ui.status)
        assertEquals(now - 5 * 60_000L, ui.lastSyncedAtMillis)
        assertEquals(2, ui.pending)
        assertEquals(1, ui.failed)
        assertEquals(now, ui.nowMillis)
        job.cancel()
    }

    @Test
    fun `sync now asks the ledger for a full sync`() = runTest(dispatcher) {
        val viewModel = SettingsViewModel(ledger, outbox, clock)

        viewModel.onSyncNow()
        advanceUntilIdle()

        assertEquals(1, ledger.syncNowCalls)
    }

    @Test
    fun `summary reflects the last sync age`() {
        val now = 1_800_000_000_000L
        assertEquals("Not synced yet", syncSummary(SyncStatusUi(nowMillis = now)))
        assertEquals(
            "Synced just now",
            syncSummary(SyncStatusUi(lastSyncedAtMillis = now, nowMillis = now)),
        )
        assertEquals(
            "Synced 5m ago",
            syncSummary(SyncStatusUi(lastSyncedAtMillis = now - 5 * 60_000L, nowMillis = now)),
        )
        assertEquals(
            "Synced 3h ago",
            syncSummary(SyncStatusUi(lastSyncedAtMillis = now - 3 * 3_600_000L, nowMillis = now)),
        )
        assertEquals(
            "Synced 2d ago",
            syncSummary(SyncStatusUi(lastSyncedAtMillis = now - 2 * 86_400_000L, nowMillis = now)),
        )
    }

    @Test
    fun `summary reflects failures and queued changes`() {
        assertEquals(
            "Syncing…",
            syncSummary(SyncStatusUi(status = SyncStatus.REFRESHING)),
        )
        assertEquals(
            "Offline · showing cached data · 3 waiting",
            syncSummary(SyncStatusUi(status = SyncStatus.OFFLINE, pending = 3)),
        )
        assertEquals(
            "Sync failed · 1 couldn't sync",
            syncSummary(SyncStatusUi(status = SyncStatus.FAILED, failed = 1)),
        )
    }

    private class FakeLedgerSync : LedgerSync {
        override val state = MutableStateFlow(SyncState())
        var syncNowCalls = 0

        override suspend fun refresh(month: YearMonth, force: Boolean) = Unit

        override suspend fun syncNow(): Boolean {
            syncNowCalls++
            return true
        }
    }

    private class FakeOutboxSync : OutboxSync {
        override val snapshot = MutableStateFlow(OutboxSnapshot())

        override suspend fun flush(): Boolean = true

        override suspend fun retryFailed() = Unit

        override suspend fun discard(entityId: String) = Unit
    }
}
