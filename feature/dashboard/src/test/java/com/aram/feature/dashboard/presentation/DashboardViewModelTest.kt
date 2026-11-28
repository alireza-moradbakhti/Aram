package com.aram.feature.dashboard.presentation

import com.aram.core.domain.repository.TransactionRepository
import com.aram.core.domain.transaction.TransactionDraft
import com.aram.core.model.Outcome
import com.aram.core.model.Transaction
import com.aram.core.model.TransactionType
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val rows = MutableStateFlow<List<Transaction>>(emptyList())
    private val repository = FakeRepository(rows)
    private val clock = Clock.fixed(
        ZonedDateTime.of(2026, 11, 28, 10, 0, 0, 0, ZoneOffset.UTC).toInstant(),
        ZoneOffset.UTC,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `starts in the loading state`() = runTest(dispatcher) {
        val viewModel = DashboardViewModel(repository, clock)

        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `keeps loading until the placeholder delay elapses`() = runTest(dispatcher) {
        val viewModel = DashboardViewModel(repository, clock)

        advanceTimeBy(300)

        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `settles to content once the load completes`() = runTest(dispatcher) {
        val viewModel = DashboardViewModel(repository, clock)

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `trend buckets the last six months on the injected clock`() = runTest(dispatcher) {
        rows.value = listOf(
            transaction(TransactionType.EXPENSE, 300_000, at(2026, 11, 5, 9, 0)),
            transaction(TransactionType.INCOME, 5_000_000, at(2026, 11, 1, 8, 0)),
            transaction(TransactionType.EXPENSE, 900_000, at(2026, 10, 20, 12, 0)),
            transaction(TransactionType.TRANSFER, 700_000, at(2026, 10, 2, 12, 0)),
            transaction(TransactionType.EXPENSE, 123_000, at(2026, 4, 30, 12, 0)), // before June
        )
        val viewModel = DashboardViewModel(repository, clock)

        advanceUntilIdle()
        val trend = viewModel.uiState.value.trend

        assertEquals(listOf("Jun", "Jul", "Aug", "Sep", "Oct", "Nov"), trend.buckets.map { it.label })
        assertEquals(listOf(0L, 0L, 0L, 0L, 900_000L, 300_000L), trend.buckets.map { it.expenseMinor })
        assertEquals(listOf(0L, 0L, 0L, 0L, 0L, 5_000_000L), trend.buckets.map { it.incomeMinor })
        assertFalse(trend.isEmpty)
    }

    @Test
    fun `empty history leaves every bucket at zero`() = runTest(dispatcher) {
        val viewModel = DashboardViewModel(repository, clock)

        advanceUntilIdle()

        assertEquals(6, viewModel.uiState.value.trend.buckets.size)
        assertTrue(viewModel.uiState.value.trend.isEmpty)
    }

    @Test
    fun `repository is queried with the six-month half-open window`() = runTest(dispatcher) {
        DashboardViewModel(repository, clock)
        advanceUntilIdle()

        val zone = ZoneOffset.UTC
        val expectedFrom = LocalDateTime.of(2026, 6, 1, 0, 0).atZone(zone).toInstant().toEpochMilli()
        val expectedTo = LocalDateTime.of(2026, 12, 1, 0, 0).atZone(zone).toInstant().toEpochMilli()
        assertEquals(expectedFrom, repository.lastFrom)
        assertEquals(expectedTo, repository.lastTo)
    }

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        ZonedDateTime.of(LocalDateTime.of(year, month, day, hour, minute), ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()

    private fun transaction(type: TransactionType, amountMinor: Long, occurredAt: Long) =
        Transaction(
            id = "t-$occurredAt-$type",
            accountId = "acc",
            categoryId = null,
            type = type,
            amountMinor = amountMinor,
            currencyCode = "IRR",
            occurredAt = occurredAt,
            note = null,
            transferGroupId = null,
            recurringRuleId = null,
        )

    private class FakeRepository(
        private val rows: MutableStateFlow<List<Transaction>>,
    ) : TransactionRepository {

        var lastFrom: Long? = null
        var lastTo: Long? = null

        override fun observeTransactions(fromInclusive: Long, toExclusive: Long): Flow<List<Transaction>> {
            lastFrom = fromInclusive
            lastTo = toExclusive
            return rows.map { all ->
                all.filter { it.occurredAt in fromInclusive until toExclusive }
            }
        }

        override fun observeRecent(limit: Int): Flow<List<Transaction>> = rows

        override fun observeTransaction(id: String): Flow<Transaction?> = rows.map { all ->
            all.firstOrNull { it.id == id }
        }

        override suspend fun create(draft: TransactionDraft): Outcome<Transaction> = error("unused")

        override suspend fun update(id: String, draft: TransactionDraft): Outcome<Transaction> = error("unused")

        override suspend fun softDelete(id: String): Outcome<Unit> = error("unused")
    }
}