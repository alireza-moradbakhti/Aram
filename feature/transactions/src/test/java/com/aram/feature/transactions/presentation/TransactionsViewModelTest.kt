package com.aram.feature.transactions.presentation

import app.cash.turbine.test
import com.aram.core.domain.repository.LedgerDirectory
import com.aram.core.domain.repository.LedgerSync
import com.aram.core.domain.repository.OutboxSnapshot
import com.aram.core.domain.repository.OutboxSync
import com.aram.core.domain.repository.TransactionRepository
import com.aram.core.domain.transaction.TransactionDraft
import com.aram.core.domain.transaction.TransactionFilter
import com.aram.core.domain.transaction.TransferDraft
import com.aram.core.model.Account
import com.aram.core.model.AccountType
import com.aram.core.model.AppError
import com.aram.core.model.Category
import com.aram.core.model.CategoryKind
import com.aram.core.model.Outcome
import com.aram.core.model.SyncState
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
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val clock: Clock = Clock.fixed(Instant.parse("2026-10-12T12:00:00Z"), ZoneOffset.UTC)
    private val repository = FakeRangeRepository()
    private val events = mutableListOf<String>()
    private val ledgerSync = FakeLedgerSync(events)
    private val outbox = FakeOutboxSync(events)
    private val directory = FakeDirectory(
        accounts = MutableStateFlow(listOf(cash, card)),
        categories = MutableStateFlow(listOf(food, transport, salary)),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = TransactionsViewModel(repository, directory, ledgerSync, outbox, clock)

    @Test
    fun `uiState streams loading then the loaded month`() = runTest(dispatcher) {
        repository.rows.value = listOf(
            expense("a", 18_000, "food", "2026-10-12T09:12:00Z"),
        )
        val viewModel = viewModel()

        viewModel.uiState.test {
            assertTrue(awaitItem().loading)
            advanceUntilIdle()
            val loaded = awaitItem()
            assertTrue(!loaded.loading)
            assertEquals(listOf("a"), loaded.groups.flatMap { it.transactions }.map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a scope change streams the filtered rows`() = runTest(dispatcher) {
        repository.rows.value = listOf(
            expense("expense", 18_000, "food", "2026-10-12T09:12:00Z"),
            income("income", 8_500_000, "salary", "2026-10-12T08:00:00Z"),
        )
        val viewModel = viewModel()

        viewModel.uiState.test {
            awaitItem()
            advanceUntilIdle()
            awaitItem()
            viewModel.onScopeChange(TransactionScope.INCOME)
            advanceUntilIdle()

            val filtered = awaitItem()
            assertEquals(listOf("income"), filtered.groups.flatMap { it.transactions }.map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `rows group by day newest first with newest rows first`() = runTest(dispatcher) {
        repository.rows.value = listOf(
            expense("a", 18_000, "food", "2026-10-12T09:12:00Z"),
            expense("b", 24_000, "transport", "2026-10-12T08:40:00Z"),
            income("c", 8_500_000, "salary", "2026-10-12T08:00:00Z"),
            expense("d", 124_000, "food", "2026-10-11T18:20:00Z"),
        )
        val viewModel = viewModel()
        advanceUntilIdle()

        val groups = viewModel.uiState.value.groups
        assertEquals(listOf(LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 11)), groups.map { it.date })
        assertEquals(listOf("a", "b", "c"), groups[0].transactions.map { it.id })
        assertEquals("09:12 · Cash", groups[0].transactions[0].detail)
        assertEquals(-42_000, groups[0].totalMinor)
        assertEquals(18_000, groups[0].transactions[0].amountMinor * -1)
        assertTrue(groups[0].transactions[2].incoming)
    }

    @Test
    fun `opening the screen refreshes the visible month without forcing`() = runTest(dispatcher) {
        viewModel()
        advanceUntilIdle()

        assertEquals(listOf(YearMonth.of(2026, 10) to false), ledgerSync.refreshes)
    }

    @Test
    fun `pull to refresh forces the current month`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()
        ledgerSync.refreshes.clear()
        events.clear()

        viewModel.onRefresh()
        advanceUntilIdle()

        assertEquals(listOf(YearMonth.of(2026, 10) to true), ledgerSync.refreshes)
        assertEquals(listOf("flush", "refresh"), events)
    }

    @Test
    fun `the queued and failed counts drive the banner state`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        outbox.snapshot.value = OutboxSnapshot(pending = 3, failed = 1)
        advanceUntilIdle()

        assertEquals(OutboxSnapshot(pending = 3, failed = 1), viewModel.uiState.value.outbox)
    }

    @Test
    fun `retrying queued writes clears failures before pushing`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()
        events.clear()

        viewModel.onRetryQueued()
        advanceUntilIdle()

        assertEquals(1, outbox.retries)
        assertEquals(1, outbox.flushes)
        assertEquals(listOf("retry", "flush", "refresh"), events)
    }

    @Test
    fun `a locally queued row is marked for sync`() = runTest(dispatcher) {
        repository.rows.value = listOf(
            expense("queued", 18_000, "food", "2026-10-12T09:12:00Z").copy(pendingPush = true),
        )
        val viewModel = viewModel()
        advanceUntilIdle()

        val row = viewModel.uiState.value.groups.single().transactions.single()
        assertTrue(row.pendingSync)
    }

    @Test
    fun `an income-only day reports a positive day total`() = runTest(dispatcher) {
        repository.rows.value = listOf(
            income("salary", 8_500_000, "salary", "2026-10-12T08:00:00Z"),
        )
        val viewModel = viewModel()
        advanceUntilIdle()

        assertEquals(8_500_000, viewModel.uiState.value.groups.single().totalMinor)
    }

    @Test
    fun `month query covers the local month boundaries`() = runTest(dispatcher) {
        repository.rows.value = listOf(
            expense("first", 1_000, "food", "2026-10-01T00:00:00Z"),
            expense("previous", 2_000, "food", "2026-09-30T23:59:59Z"),
            expense("next", 3_000, "food", "2026-11-01T00:00:00Z"),
        )
        val viewModel = viewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf("first"), state.groups.flatMap { it.transactions }.map { it.id })
        assertEquals(1_000, state.spentMinor)
        assertEquals(2, repository.ranges.size)
        assertEquals(Instant.parse("2026-10-01T00:00:00Z").toEpochMilli(), repository.ranges[0].first)
        assertEquals(Instant.parse("2026-11-01T00:00:00Z").toEpochMilli(), repository.ranges[0].second)
        assertEquals(Instant.parse("2026-09-01T00:00:00Z").toEpochMilli(), repository.ranges[1].first)
        assertEquals(Instant.parse("2026-10-01T00:00:00Z").toEpochMilli(), repository.ranges[1].second)
    }

    @Test
    fun `scope filters rows but month totals stay complete`() = runTest(dispatcher) {
        repository.rows.value = listOf(
            expense("expense", 18_000, "food", "2026-10-12T09:12:00Z"),
            income("income", 8_500_000, "salary", "2026-10-12T08:00:00Z"),
        )
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onScopeChange(TransactionScope.EXPENSES)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf("expense"), state.groups.flatMap { it.transactions }.map { it.id })
        assertEquals(2, state.entryCount)
        assertEquals(18_000, state.spentMinor)
        assertEquals(8_500_000, state.incomeMinor)
        assertEquals(1, state.incomeCount)
    }

    @Test
    fun `empty month is flagged and totals stay zero`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isEmpty)
        assertEquals(0, state.entryCount)
        assertEquals(0, state.spentMinor)
        assertNull(state.previousSpentMinor)
    }

    @Test
    fun `month navigation reloads the ledger for the new range`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()
        assertEquals(YearMonth.of(2026, 10), viewModel.uiState.value.month)

        viewModel.onPreviousMonth()
        advanceUntilIdle()

        assertEquals(YearMonth.of(2026, 9), viewModel.uiState.value.month)
        assertTrue(
            repository.ranges.contains(
                Instant.parse("2026-09-01T00:00:00Z").toEpochMilli() to
                    Instant.parse("2026-10-01T00:00:00Z").toEpochMilli(),
            ),
        )
    }

    @Test
    fun `totals use the dominant currency of the month`() = runTest(dispatcher) {
        repository.rows.value = listOf(
            expense("usd", 20_000, "food", "2026-10-12T09:00:00Z"),
            expense("usd2", 30_000, "food", "2026-10-11T09:00:00Z"),
            expense("eur", 99_000, "food", "2026-10-10T09:00:00Z", currency = "EUR"),
        )
        val viewModel = viewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(CurrencyCode.USD, state.currency)
        assertEquals(50_000, state.spentMinor)
        assertEquals(3, state.entryCount)
    }

    @Test
    fun `note replaces the category as the row title`() = runTest(dispatcher) {
        repository.rows.value = listOf(
            expense("a", 18_000, "food", "2026-10-12T09:12:00Z", note = "Coffee with Nima"),
        )
        val viewModel = viewModel()
        advanceUntilIdle()

        val row = viewModel.uiState.value.groups.single().transactions.single()
        assertEquals("Coffee with Nima", row.title)
        assertEquals("Food & Drinks · 09:12 · Cash", row.detail)
    }

    @Test
    fun `category-only rows do not repeat the category in the detail`() = runTest(dispatcher) {
        repository.rows.value = listOf(
            expense("a", 18_000, "food", "2026-10-12T09:12:00Z"),
        )
        val viewModel = viewModel()
        advanceUntilIdle()

        val row = viewModel.uiState.value.groups.single().transactions.single()
        assertEquals("Food & Drinks", row.title)
        assertEquals("09:12 · Cash", row.detail)
    }

    @Test
    fun `a transfer pair renders as one outflow row and stays out of the summaries`() = runTest(dispatcher) {
        repository.rows.value = listOf(
            expense("a", 18_000, "food", "2026-10-12T09:12:00Z"),
            transfer("out", -50_000, cash.id, "2026-10-12T10:00:00Z", "g1", note = "Rent"),
            transfer("in", 50_000, card.id, "2026-10-12T10:00:00Z", "g1", note = "Rent"),
            income("c", 8_500_000, "salary", "2026-10-12T08:00:00Z"),
        )
        val viewModel = viewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(18_000, state.spentMinor)
        assertEquals(8_500_000, state.incomeMinor)
        assertEquals(3, state.entryCount)
        assertEquals(1, state.incomeCount)

        val group = state.groups.single()
        assertEquals(listOf("out", "a", "c"), group.transactions.map { it.id })
        assertEquals(-68_000, group.totalMinor)

        val row = group.transactions.first { it.id == "out" }
        assertEquals("Rent", row.title)
        assertEquals("Transfer · 10:00 · Card", row.detail)
        assertEquals(-50_000, row.amountMinor)
    }

    @Test
    fun `a transfer only month reports no spending or income`() = runTest(dispatcher) {
        repository.rows.value = listOf(
            transfer("out", -50_000, cash.id, "2026-10-12T10:00:00Z", "g1", note = null),
            transfer("in", 50_000, card.id, "2026-10-12T10:00:00Z", "g1", note = null),
        )
        val viewModel = viewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(0, state.spentMinor)
        assertEquals(0, state.incomeMinor)
        assertEquals(1, state.entryCount)
        assertTrue(state.groups.single().transactions.single().pendingSync.not())
        assertEquals(-50_000, state.groups.single().totalMinor)
    }

    @Test
    fun `typing debounces the search until the pause`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onSearchQueryChange("co")
        advanceTimeBy(100)
        viewModel.onSearchQueryChange("coffee")
        advanceTimeBy(100)
        assertEquals(0, repository.searches.size)

        advanceUntilIdle()
        assertEquals(1, repository.searches.size)
        assertEquals("coffee", repository.searches.single().query)
        assertTrue(viewModel.uiState.value.isFiltering)
        assertEquals("coffee", viewModel.uiState.value.query)
    }

    @Test
    fun `clearing the query returns to the month view`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onSearchQueryChange("coffee")
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isFiltering)
        val searchesBeforeClear = repository.searches.size

        viewModel.onSearchQueryChange("")
        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.isFiltering)
        assertEquals(searchesBeforeClear, repository.searches.size)
    }

    @Test
    fun `search reaches across months`() = runTest(dispatcher) {
        repository.rows.value = listOf(
            expense("oct", 18_000, "food", "2026-10-12T09:12:00Z", note = "Coffee"),
            expense("sep", 9_000, "food", "2026-09-02T08:00:00Z", note = "Coffee beans"),
            expense("tea", 5_000, "food", "2026-09-03T08:00:00Z", note = "Tea"),
        )
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onSearchQueryChange("coffee")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isFiltering)
        assertEquals(2, state.entryCount)
        assertEquals(2, state.groups.flatMap { it.transactions }.size)
        assertEquals(false, state.noMatches)
    }

    @Test
    fun `expenses scope becomes an income exclusion in the search`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onScopeChange(TransactionScope.EXPENSES)
        viewModel.onSearchQueryChange("ride")
        advanceUntilIdle()

        val filter = repository.searches.last()
        assertEquals(null, filter.type)
        assertEquals(TransactionType.INCOME, filter.excludeType)
        assertEquals(TransactionScope.EXPENSES, viewModel.uiState.value.scope)
    }

    @Test
    fun `applied filters empty the results and flag no matches`() = runTest(dispatcher) {
        repository.rows.value = listOf(
            expense("a", 18_000, "food", "2026-10-12T09:12:00Z", note = "Coffee"),
        )
        val viewModel = viewModel()
        advanceUntilIdle()
        assertEquals(false, viewModel.uiState.value.noMatches)

        viewModel.onApplyFilters(TransactionFiltersUi(categoryId = "transport"))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isFiltering)
        assertTrue(state.noMatches)
        assertEquals(0, state.entryCount)
        assertEquals("transport", repository.searches.last().categoryId)
    }

    @Test
    fun `date presets map to half-open local bounds`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onApplyFilters(TransactionFiltersUi(dateRange = DateRangePreset.LAST_30_DAYS))
        advanceUntilIdle()

        val filter = repository.searches.last()
        assertEquals(Instant.parse("2026-09-13T00:00:00Z").toEpochMilli(), filter.fromInclusive)
        assertEquals(Instant.parse("2026-10-13T00:00:00Z").toEpochMilli(), filter.toExclusive)

        viewModel.onApplyFilters(TransactionFiltersUi(dateRange = DateRangePreset.THIS_YEAR))
        advanceUntilIdle()

        val year = repository.searches.last()
        assertEquals(Instant.parse("2026-01-01T00:00:00Z").toEpochMilli(), year.fromInclusive)
        assertEquals(Instant.parse("2027-01-01T00:00:00Z").toEpochMilli(), year.toExclusive)
    }

    @Test
    fun `clear all resets the query, filters and scope`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onSearchQueryChange("coffee")
        viewModel.onScopeChange(TransactionScope.INCOME)
        viewModel.onApplyFilters(
            TransactionFiltersUi(categoryId = "food", dateRange = DateRangePreset.THIS_YEAR),
        )
        advanceUntilIdle()

        viewModel.onClearFilters()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isFiltering)
        assertEquals(TransactionScope.ALL, state.scope)
        assertEquals(TransactionFiltersUi(), state.filters)
        assertEquals("", state.query)
    }

    @Test
    fun `an empty month is not a no-match state`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isFiltering)
        assertEquals(false, state.noMatches)
        assertTrue(state.isEmpty)
    }

    private fun expense(
        id: String,
        amount: Long,
        categoryId: String,
        at: String,
        note: String? = null,
        currency: String = "USD",
    ) = transaction(id, TransactionType.EXPENSE, amount, categoryId, at, note, currency)

    private fun income(
        id: String,
        amount: Long,
        categoryId: String,
        at: String,
        note: String? = null,
        currency: String = "USD",
    ) = transaction(id, TransactionType.INCOME, amount, categoryId, at, note, currency)

    private fun transfer(
        id: String,
        amount: Long,
        accountId: String,
        at: String,
        groupId: String,
        note: String?,
    ) = Transaction(
        id = id,
        accountId = accountId,
        categoryId = null,
        type = TransactionType.TRANSFER,
        amountMinor = amount,
        currencyCode = "USD",
        occurredAt = Instant.parse(at).toEpochMilli(),
        note = note,
        transferGroupId = groupId,
        recurringRuleId = null,
    )

    private fun transaction(
        id: String,
        type: TransactionType,
        amount: Long,
        categoryId: String,
        at: String,
        note: String?,
        currency: String,
    ) = Transaction(
        id = id,
        accountId = cash.id,
        categoryId = categoryId,
        type = type,
        amountMinor = amount,
        currencyCode = currency,
        occurredAt = Instant.parse(at).toEpochMilli(),
        note = note,
        transferGroupId = null,
        recurringRuleId = null,
    )

    private class FakeDirectory(
        private val accounts: MutableStateFlow<List<Account>>,
        private val categories: MutableStateFlow<List<Category>>,
    ) : LedgerDirectory {
        override fun observeAccounts(): Flow<List<Account>> = accounts
        override fun observeCategories(): Flow<List<Category>> = categories
    }

    private class FakeRangeRepository : TransactionRepository {

        val rows = MutableStateFlow<List<Transaction>>(emptyList())
        val ranges = mutableListOf<Pair<Long, Long>>()
        val searches = mutableListOf<TransactionFilter>()

        override fun observeTransactions(fromInclusive: Long, toExclusive: Long): Flow<List<Transaction>> {
            ranges += fromInclusive to toExclusive
            return rows.map { all -> all.filter { it.occurredAt in fromInclusive until toExclusive } }
        }

        override fun search(filter: TransactionFilter): Flow<List<Transaction>> {
            searches += filter
            return rows.map { all ->
                all.filter { tx ->
                    val from = filter.fromInclusive
                    val to = filter.toExclusive
                    (filter.query.isEmpty() || tx.note?.contains(filter.query, ignoreCase = true) == true) &&
                        (filter.type == null || tx.type == filter.type) &&
                        (filter.excludeType == null || tx.type != filter.excludeType) &&
                        (filter.categoryId == null || tx.categoryId == filter.categoryId) &&
                        (from == null || tx.occurredAt >= from) &&
                        (to == null || tx.occurredAt < to)
                }
            }
        }

        override fun observeRecent(limit: Int): Flow<List<Transaction>> = rows

        override fun observeTransaction(id: String): Flow<Transaction?> = rows.map { all ->
            all.firstOrNull { it.id == id }
        }

        override suspend fun create(draft: TransactionDraft): Outcome<Transaction> =
            Outcome.Failure(AppError.Storage())

        override suspend fun createTransfer(draft: TransferDraft): Outcome<String> =
            Outcome.Failure(AppError.Storage())

        override suspend fun update(id: String, draft: TransactionDraft): Outcome<Transaction> =
            Outcome.Failure(AppError.Storage())

        override suspend fun softDelete(id: String): Outcome<Unit> = Outcome.Success(Unit)

        override suspend fun restore(id: String): Outcome<Unit> = Outcome.Success(Unit)
    }

    private class FakeLedgerSync(private val events: MutableList<String>) : LedgerSync {
        override val state = MutableStateFlow(SyncState())
        val refreshes = mutableListOf<Pair<YearMonth, Boolean>>()

        override suspend fun refresh(month: YearMonth, force: Boolean) {
            refreshes += month to force
            events += "refresh"
        }

        override suspend fun syncNow(): Boolean {
            events += "syncNow"
            return true
        }
    }

    private class FakeOutboxSync(private val events: MutableList<String>) : OutboxSync {
        override val snapshot = MutableStateFlow(OutboxSnapshot())
        var flushes = 0
        var retries = 0

        override suspend fun flush(): Boolean {
            flushes++
            events += "flush"
            return true
        }

        override suspend fun retryFailed() {
            retries++
            events += "retry"
        }

        override suspend fun discard(entityId: String) = Unit
    }

    private companion object {
        val cash = Account(
            id = "cash",
            name = "Cash",
            type = AccountType.CASH,
            currencyCode = "USD",
            openingBalanceMinor = 0L,
            colorArgb = 0,
            iconKey = "wallet",
            archived = false,
        )
        val card = Account(
            id = "card",
            name = "Card",
            type = AccountType.BANK,
            currencyCode = "USD",
            openingBalanceMinor = 0L,
            colorArgb = 0,
            iconKey = "wallet",
            archived = false,
        )
        val food = Category(
            id = "food",
            name = "Food & Drinks",
            kind = CategoryKind.EXPENSE,
            parentId = null,
            colorArgb = 0,
            iconKey = "food",
            archived = false,
        )
        val transport = Category(
            id = "transport",
            name = "Transport",
            kind = CategoryKind.EXPENSE,
            parentId = null,
            colorArgb = 0,
            iconKey = "transport",
            archived = false,
        )
        val salary = Category(
            id = "salary",
            name = "Salary",
            kind = CategoryKind.INCOME,
            parentId = null,
            colorArgb = 0,
            iconKey = "salary",
            archived = false,
        )
    }
}
