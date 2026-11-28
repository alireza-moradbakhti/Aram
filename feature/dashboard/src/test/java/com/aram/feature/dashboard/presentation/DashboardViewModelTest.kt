package com.aram.feature.dashboard.presentation

import com.aram.core.domain.repository.LedgerDirectory
import com.aram.core.domain.repository.TransactionRepository
import com.aram.core.domain.transaction.TransactionDraft
import com.aram.core.model.Account
import com.aram.core.model.AccountType
import com.aram.core.model.Category
import com.aram.core.model.Outcome
import com.aram.core.model.Transaction
import com.aram.core.model.TransactionType
import com.aram.core.money.CurrencyCode
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeTransactionRepository()
    private val directory = FakeDirectory()
    private val clock = Clock.fixed(Instant.parse("2026-11-15T12:00:00Z"), ZoneOffset.UTC)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = DashboardViewModel(repository, directory, clock)

    private fun nov(day: Int) = LocalDate.of(2026, 11, day)
        .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    private fun oct(day: Int) = LocalDate.of(2026, 10, day)
        .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    @Test
    fun `starts in the loading state`() = runTest(dispatcher) {
        val viewModel = viewModel()

        assertTrue(viewModel.uiState.value.isLoading)
        assertFalse(viewModel.uiState.value.hasError)
    }

    @Test
    fun `loads current month totals and previous month deltas`() = runTest(dispatcher) {
        repository.rows.value = listOf(
            transaction("e1", TransactionType.EXPENSE, 18_644_000, nov(3)),
            transaction("i1", TransactionType.INCOME, 45_000_000, nov(1)),
            transaction("t1", TransactionType.TRANSFER, -500_000, nov(4)),
            transaction("t2", TransactionType.TRANSFER, 500_000, nov(4)),
            transaction("e2", TransactionType.EXPENSE, 20_000_000, oct(5)),
            transaction("i2", TransactionType.INCOME, 40_000_000, oct(1)),
        )
        val viewModel = viewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.hasError)
        assertEquals(YearMonth.of(2026, 11), state.month)
        assertEquals(18_644_000L, state.spent?.amountMinor)
        assertEquals(26_356_000L, state.saved?.amountMinor)
        assertEquals(26_356_000L, state.safeToSpend?.total?.amountMinor)
        assertEquals(16, state.safeToSpend?.daysRemaining)
        assertEquals(1_647_250L, state.safeToSpend?.dailyAmount?.amountMinor)
        assertEquals(20_000_000L, state.previousSpent?.amountMinor)
        assertEquals(20_000_000L, state.previousSaved?.amountMinor)
        assertFalse(state.isEmpty)
    }

    @Test
    fun `an empty month reports the first-run state`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isEmpty)
        assertEquals(0L, state.spent?.amountMinor)
        assertEquals(0L, state.saved?.amountMinor)
        assertEquals(0L, state.safeToSpend?.dailyAmount?.amountMinor)
        assertNull(state.previousSpent)
        assertNull(state.previousSaved)
    }

    @Test
    fun `uses the transaction currency for formatting`() = runTest(dispatcher) {
        repository.rows.value = listOf(
            transaction("u1", TransactionType.EXPENSE, 1_850, nov(3), currency = "USD"),
            transaction("u2", TransactionType.INCOME, 5_000, nov(1), currency = "USD"),
        )
        val viewModel = viewModel()
        advanceUntilIdle()

        assertEquals(CurrencyCode.USD, viewModel.uiState.value.spent?.currency)
    }

    @Test
    fun `a repository failure surfaces as an error and retry recovers`() = runTest(dispatcher) {
        repository.failure = IllegalStateException("boom")
        val viewModel = viewModel()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.hasError)
        assertFalse(viewModel.uiState.value.isLoading)

        repository.failure = null
        repository.rows.value = listOf(
            transaction("e1", TransactionType.EXPENSE, 1_000, nov(3)),
        )
        viewModel.retry()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.hasError)
        assertEquals(1_000L, viewModel.uiState.value.spent?.amountMinor)
    }

    private fun transaction(
        id: String,
        type: TransactionType,
        amountMinor: Long,
        occurredAt: Long,
        currency: String = "IRR",
    ) = Transaction(
        id = id,
        accountId = "cash",
        categoryId = null,
        type = type,
        amountMinor = amountMinor,
        currencyCode = currency,
        occurredAt = occurredAt,
        note = null,
        transferGroupId = if (type == TransactionType.TRANSFER) "group-$id" else null,
        recurringRuleId = null,
    )

    private class FakeTransactionRepository : TransactionRepository {

        val rows = MutableStateFlow<List<Transaction>>(emptyList())
        var failure: Throwable? = null

        override fun observeTransactions(
            fromInclusive: Long,
            toExclusive: Long,
        ): Flow<List<Transaction>> = rows.map { list ->
            failure?.let { throw it }
            list.filter { it.occurredAt in fromInclusive until toExclusive }
        }

        override fun observeRecent(limit: Int): Flow<List<Transaction>> =
            rows.map { it.sortedByDescending(Transaction::occurredAt).take(limit) }

        override fun observeTransaction(id: String): Flow<Transaction?> =
            rows.map { list -> list.firstOrNull { it.id == id } }

        override suspend fun create(draft: TransactionDraft): Outcome<Transaction> =
            error("unused")

        override suspend fun update(id: String, draft: TransactionDraft): Outcome<Transaction> =
            error("unused")

        override suspend fun softDelete(id: String): Outcome<Unit> = error("unused")
    }

    private class FakeDirectory : LedgerDirectory {

        private val accounts = MutableStateFlow(
            listOf(
                Account(
                    id = "cash",
                    name = "Cash",
                    type = AccountType.CASH,
                    currencyCode = "IRR",
                    openingBalanceMinor = 0,
                    colorArgb = 0xFF0E9F6E.toInt(),
                    iconKey = "wallet",
                    archived = false,
                ),
            ),
        )
        private val categories = MutableStateFlow<List<Category>>(emptyList())

        override fun observeAccounts(): Flow<List<Account>> = accounts

        override fun observeCategories(): Flow<List<Category>> = categories
    }
}