package com.aram.feature.dashboard.presentation

import com.aram.core.domain.repository.LedgerDirectory
import com.aram.core.domain.repository.TransactionRepository
import com.aram.core.domain.transaction.TransactionDraft
import com.aram.core.model.Account
import com.aram.core.model.AccountType
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
import kotlinx.coroutines.test.StandardTestDispatcher
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
    private val clock: Clock = Clock.fixed(Instant.parse("2026-11-12T09:00:00Z"), ZoneOffset.UTC)

    private val groceries = category("c-groceries", "Groceries", "groceries")
    private val salary = category("c-salary", "Salary", "salary", CategoryKind.INCOME)
    private val mainAccount = account("a1", "Main account")

    private lateinit var repository: FakeTransactionRepository
    private lateinit var directory: FakeLedgerDirectory

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeTransactionRepository()
        directory = FakeLedgerDirectory(
            categories = listOf(groceries, salary),
            accounts = listOf(mainAccount),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `starts in the loading state`() = runTest(dispatcher) {
        val viewModel = DashboardViewModel(repository, directory, clock)

        assertTrue(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.recent.isEmpty())
    }

    @Test
    fun `maps the newest rows in ledger order`() = runTest(dispatcher) {
        repository.recent.value = listOf(
            expense("t3", note = "Café Nima", amount = 180_000, at = instant("2026-11-12T09:12:00Z")),
            expense("t2", note = null, amount = 500_000, at = instant("2026-11-11T08:04:00Z")),
            income("t1", amount = 85_000_000, at = instant("2026-11-10T07:00:00Z")),
        )

        val viewModel = DashboardViewModel(repository, directory, clock)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(listOf("t3", "t2", "t1"), state.recent.map { it.id })
        assertEquals("Café Nima", state.recent[0].title)
        assertEquals("Groceries · 09:12", state.recent[0].meta)
        assertEquals(-180_000L, state.recent[0].amountMinor)
        assertEquals("groceries", state.recent[0].categoryIconKey)
        assertEquals("Groceries", state.recent[1].title)
        assertEquals("Nov 11", state.recent[1].meta.substringAfter(" · "))
        assertEquals("Salary", state.recent[2].title)
        assertEquals(85_000_000L, state.recent[2].amountMinor)
        assertTrue(state.recent[2].incoming)
    }

    @Test
    fun `labels transfers with the source account`() = runTest(dispatcher) {
        repository.recent.value = listOf(
            transfer("t1", note = null, amount = 2_000_000, at = instant("2026-11-12T10:00:00Z")),
        )

        val viewModel = DashboardViewModel(repository, directory, clock)
        advanceUntilIdle()

        val row = viewModel.uiState.value.recent.single()
        assertEquals("Transfer", row.title)
        assertEquals("10:00 · Main account", row.meta)
        assertEquals(-2_000_000L, row.amountMinor)
        assertFalse(row.incoming)
    }

    @Test
    fun `only the recent slice is observed, never the full ledger`() = runTest(dispatcher) {
        DashboardViewModel(repository, directory, clock)
        advanceUntilIdle()

        assertEquals(5, repository.lastRecentLimit)
        assertEquals(0, repository.observeTransactionsCalls)
    }

    @Test
    fun `an empty ledger yields an empty recent list`() = runTest(dispatcher) {
        val viewModel = DashboardViewModel(repository, directory, clock)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.recent.isEmpty())
    }

    @Test
    fun `fraction digits follow the row currency`() = runTest(dispatcher) {
        repository.recent.value = listOf(
            expense("eur", note = "Train", amount = 12_50, at = instant("2026-11-12T09:00:00Z"))
                .copy(currencyCode = "EUR"),
            expense("irr", note = "Taxi", amount = 500_000, at = instant("2026-11-12T08:00:00Z")),
        )

        val viewModel = DashboardViewModel(repository, directory, clock)
        advanceUntilIdle()

        val rows = viewModel.uiState.value.recent
        assertEquals(2, rows[0].fractionDigits)
        assertEquals(0, rows[1].fractionDigits)
    }

    private fun expense(id: String, note: String?, amount: Long, at: Long) =
        transaction(id, TransactionType.EXPENSE, amount, at, note, categoryId = groceries.id)

    private fun income(id: String, amount: Long, at: Long) =
        transaction(id, TransactionType.INCOME, amount, at, note = "Salary", categoryId = salary.id)

    private fun transfer(id: String, note: String?, amount: Long, at: Long) =
        transaction(
            id, TransactionType.TRANSFER, amount, at, note,
            categoryId = null, transferGroupId = "g1",
        )

    private fun transaction(
        id: String,
        type: TransactionType,
        amountMinor: Long,
        occurredAt: Long,
        note: String?,
        categoryId: String?,
        transferGroupId: String? = null,
    ) = Transaction(
        id = id,
        accountId = mainAccount.id,
        categoryId = categoryId,
        type = type,
        amountMinor = amountMinor,
        currencyCode = "IRR",
        occurredAt = occurredAt,
        note = note,
        transferGroupId = transferGroupId,
        recurringRuleId = null,
    )

    private fun instant(iso: String): Long = Instant.parse(iso).toEpochMilli()

    private fun category(
        id: String,
        name: String,
        iconKey: String,
        kind: CategoryKind = CategoryKind.EXPENSE,
    ) = Category(
        id = id,
        name = name,
        kind = kind,
        parentId = null,
        colorArgb = 0,
        iconKey = iconKey,
        archived = false,
    )

    private fun account(id: String, name: String) = Account(
        id = id,
        name = name,
        type = AccountType.BANK,
        currencyCode = "IRR",
        openingBalanceMinor = 0,
        colorArgb = 0,
        iconKey = "card",
        archived = false,
    )
}

private class FakeTransactionRepository : TransactionRepository {

    val recent = MutableStateFlow<List<Transaction>>(emptyList())
    var lastRecentLimit: Int? = null
    var observeTransactionsCalls = 0

    override fun observeTransactions(fromInclusive: Long, toExclusive: Long): Flow<List<Transaction>> {
        observeTransactionsCalls++
        return flowOf(emptyList())
    }

    override fun observeRecent(limit: Int): Flow<List<Transaction>> {
        lastRecentLimit = limit
        return recent
    }

    override fun observeTransaction(id: String): Flow<Transaction?> = flowOf(null)

    override suspend fun create(draft: TransactionDraft): Outcome<Transaction> = error("not used")

    override suspend fun update(id: String, draft: TransactionDraft): Outcome<Transaction> = error("not used")

    override suspend fun softDelete(id: String): Outcome<Unit> = error("not used")
}

private class FakeLedgerDirectory(
    categories: List<Category> = emptyList(),
    accounts: List<Account> = emptyList(),
) : LedgerDirectory {

    val categoryFlow = MutableStateFlow(categories)
    val accountFlow = MutableStateFlow(accounts)

    override fun observeAccounts(): Flow<List<Account>> = accountFlow

    override fun observeCategories(): Flow<List<Category>> = categoryFlow
}