package com.aram.feature.dashboard.presentation

import com.aram.core.domain.repository.LedgerDirectory
import com.aram.core.domain.repository.TransactionRepository
import com.aram.core.domain.transaction.TransactionDraft
import com.aram.core.model.Account
import com.aram.core.model.Category
import com.aram.core.model.CategoryKind
import com.aram.core.model.Outcome
import com.aram.core.model.Transaction
import com.aram.core.model.TransactionType
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
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
    private val repository = FakeRepository()
    private val categories = MutableStateFlow<List<Category>>(emptyList())
    private val clock = Clock.fixed(Instant.parse("2026-11-28T10:00:00Z"), ZoneOffset.UTC)

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
        val viewModel = viewModel()

        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `keeps loading until the placeholder delay elapses`() = runTest(dispatcher) {
        val viewModel = viewModel()

        advanceTimeBy(300)

        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `settles to content once the load completes`() = runTest(dispatcher) {
        val viewModel = viewModel()

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `donut groups current month expenses by category`() = runTest(dispatcher) {
        categories.value = listOf(
            category("food", "Groceries", color = 0xFF2FD39A.toInt()),
        )
        repository.rows.value = listOf(
            expense("a", "food", 980_000, at("2026-11-05T09:00:00Z")),
            expense("b", "food", 20_000, at("2026-11-06T09:00:00Z")),
            expense("c", null, 100_000, at("2026-11-07T09:00:00Z")),
            income("d", 5_000_000, at("2026-11-10T09:00:00Z")),
            expense("e", "food", 999_999, at("2026-10-31T09:00:00Z")),
        )
        val viewModel = viewModel()

        advanceUntilIdle()

        val donut = viewModel.uiState.value.donut
        assertEquals("November", donut.monthLabel)
        assertEquals(1_100_000L, donut.totalMinor)
        assertEquals(0, donut.fractionDigits)
        assertFalse(donut.isEmpty)
        assertEquals(listOf("Groceries", "Other"), donut.slices.map { it.label })
        assertEquals(listOf(1_000_000L, 100_000L), donut.slices.map { it.amountMinor })
        assertEquals(listOf(91, 9), donut.slices.map { it.percent })
        assertEquals(360.0, donut.slices.sumOf { it.sweepAngle.toDouble() }, 0.01)
    }

    @Test
    fun `empty month yields a zero donut without slices`() = runTest(dispatcher) {
        repository.rows.value = listOf(
            income("salary", 5_000_000, at("2026-11-10T09:00:00Z")),
        )
        val viewModel = viewModel()

        advanceUntilIdle()

        val donut = viewModel.uiState.value.donut
        assertTrue(donut.isEmpty)
        assertEquals(0L, donut.totalMinor)
        assertTrue(donut.slices.isEmpty())
    }

    private fun viewModel() = DashboardViewModel(
        repository = repository,
        directory = FakeDirectory(categories),
        clock = clock,
    )

    private fun at(instant: String) = Instant.parse(instant).toEpochMilli()

    private fun expense(id: String, categoryId: String?, amount: Long, occurredAt: Long) = Transaction(
        id = id,
        accountId = "cash",
        categoryId = categoryId,
        type = TransactionType.EXPENSE,
        amountMinor = amount,
        currencyCode = "IRR",
        occurredAt = occurredAt,
        note = null,
        transferGroupId = null,
        recurringRuleId = null,
    )

    private fun income(id: String, amount: Long, occurredAt: Long) = Transaction(
        id = id,
        accountId = "cash",
        categoryId = null,
        type = TransactionType.INCOME,
        amountMinor = amount,
        currencyCode = "IRR",
        occurredAt = occurredAt,
        note = null,
        transferGroupId = null,
        recurringRuleId = null,
    )

    private fun category(id: String, name: String, color: Int) = Category(
        id = id,
        name = name,
        kind = CategoryKind.EXPENSE,
        parentId = null,
        colorArgb = color,
        iconKey = "groceries",
        archived = false,
    )

    private class FakeRepository : TransactionRepository {
        val rows = MutableStateFlow<List<Transaction>>(emptyList())
        var lastFrom: Long = Long.MIN_VALUE
        var lastTo: Long = Long.MAX_VALUE

        override fun observeTransactions(fromInclusive: Long, toExclusive: Long): Flow<List<Transaction>> {
            lastFrom = fromInclusive
            lastTo = toExclusive
            return rows.map { list ->
                list.filter { it.occurredAt >= fromInclusive && it.occurredAt < toExclusive }
            }
        }

        override fun observeRecent(limit: Int): Flow<List<Transaction>> = rows.map { it.take(limit) }

        override fun observeTransaction(id: String): Flow<Transaction?> =
            rows.map { list -> list.firstOrNull { it.id == id } }

        override suspend fun create(draft: TransactionDraft): Outcome<Transaction> = error("unused")

        override suspend fun update(id: String, draft: TransactionDraft): Outcome<Transaction> =
            error("unused")

        override suspend fun softDelete(id: String): Outcome<Unit> = error("unused")
    }

    private class FakeDirectory(
        private val categories: MutableStateFlow<List<Category>>,
    ) : LedgerDirectory {
        override fun observeAccounts(): Flow<List<Account>> = flowOf(emptyList())
        override fun observeCategories(): Flow<List<Category>> = categories
    }
}