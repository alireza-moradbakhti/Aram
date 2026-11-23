package com.aram.feature.transactions.presentation

import com.aram.core.designsystem.state.SubmitState
import com.aram.core.domain.repository.LedgerDirectory
import com.aram.core.domain.repository.LedgerLookup
import com.aram.core.domain.repository.TransactionRepository
import com.aram.core.domain.transaction.TransactionDraft
import com.aram.core.domain.transaction.TransactionField
import com.aram.core.domain.usecase.CreateTransaction
import com.aram.core.model.Account
import com.aram.core.model.AccountType
import com.aram.core.model.AppError
import com.aram.core.model.Category
import com.aram.core.model.CategoryKind
import com.aram.core.model.Outcome
import com.aram.core.model.Transaction
import com.aram.core.model.TransactionType
import com.aram.core.money.CurrencyCode
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddTransactionViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeTransactionRepository()
    private val accounts = MutableStateFlow(listOf(cash))
    private val categories = MutableStateFlow(listOf(food, salary))

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = AddTransactionViewModel(
        createTransaction = CreateTransaction(repository, FakeLedgerLookup()),
        directory = FakeLedgerDirectory(accounts, categories),
    )

    @Test
    fun `directory emission preselects the first account and expense category`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(cash.id, state.selectedAccountId)
        assertEquals(food.id, state.selectedCategoryId)
        assertEquals(CurrencyCode.USD, state.currency)
        assertTrue(state.optionsLoaded)
        assertTrue(state.canSave)
    }

    @Test
    fun `switching to income selects the first income category`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onTypeChange(TransactionType.INCOME)

        val state = viewModel.uiState.value
        assertEquals(TransactionType.INCOME, state.type)
        assertEquals(salary.id, state.selectedCategoryId)
        assertEquals(listOf(salary.id), state.visibleCategories.map { it.id })
    }

    @Test
    fun `empty amount blocks saving with an inline error`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onSave()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(AmountInputError.Empty, state.amountError)
        assertTrue(TransactionField.AMOUNT in state.invalidFields)
        assertNull(repository.createdDraft)
        assertTrue(!state.saved)
    }

    @Test
    fun `zero amount blocks saving`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onAmountChange("0.00")
        viewModel.onSave()
        advanceUntilIdle()

        assertEquals(AmountInputError.Zero, viewModel.uiState.value.amountError)
        assertNull(repository.createdDraft)
    }

    @Test
    fun `too many fraction digits blocks saving`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onAmountChange("12.345")
        viewModel.onSave()
        advanceUntilIdle()

        assertEquals(
            AmountInputError.TooManyFractionDigits(2),
            viewModel.uiState.value.amountError,
        )
        assertNull(repository.createdDraft)
    }

    @Test
    fun `valid input saves a draft in minor units and reports success`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onAmountChange("18,50")
        viewModel.onNoteChange("Lunch")
        viewModel.onSave()
        advanceUntilIdle()

        val draft = repository.createdDraft
        assertEquals(1_850L, draft?.amountMinor)
        assertEquals("USD", draft?.currencyCode)
        assertEquals(TransactionType.EXPENSE, draft?.type)
        assertEquals(food.id, draft?.categoryId)
        assertTrue(viewModel.uiState.value.saved)
    }

    @Test
    fun `domain validation problems become field flags without saving`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        categories.value = listOf(food.copy(archived = true), salary)
        advanceUntilIdle()

        viewModel.onAmountChange("5")
        viewModel.onSave()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(TransactionField.CATEGORY in state.invalidFields)
        assertTrue(!state.saved)
    }

    @Test
    fun `repository failure becomes a typed submit failure and keeps input`() = runTest(dispatcher) {
        repository.createResult = Outcome.Failure(AppError.Storage())
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onAmountChange("5")
        viewModel.onSave()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(SubmitState.Failed(AppError.Storage()), state.submitState)
        assertEquals("5", state.amountInput)
        assertTrue(!state.saved)
    }

    @Test
    fun `long notes are truncated to the 80 character limit`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onAmountChange("5")
        viewModel.onNoteChange("x".repeat(120))
        viewModel.onSave()
        advanceUntilIdle()

        assertEquals(80, repository.createdDraft?.note?.length ?: -1)
    }

    private class FakeLedgerDirectory(
        private val accounts: MutableStateFlow<List<Account>>,
        private val categories: MutableStateFlow<List<Category>>,
    ) : LedgerDirectory {
        override fun observeAccounts(): Flow<List<Account>> = accounts
        override fun observeCategories(): Flow<List<Category>> = categories
    }

    private inner class FakeLedgerLookup : LedgerLookup {
        override suspend fun account(id: String): Outcome<Account?> =
            Outcome.Success(accounts.value.firstOrNull { it.id == id })

        override suspend fun category(id: String): Outcome<Category?> =
            Outcome.Success(categories.value.firstOrNull { it.id == id })
    }

    private class FakeTransactionRepository : TransactionRepository {

        var createdDraft: TransactionDraft? = null
        var createResult: Outcome<Transaction> = Outcome.Success(saved)

        override fun observeTransactions(
            fromInclusive: Long,
            toExclusive: Long,
        ): Flow<List<Transaction>> = flowOf(emptyList())

        override fun observeRecent(limit: Int): Flow<List<Transaction>> = flowOf(emptyList())

        override fun observeTransaction(id: String): Flow<Transaction?> = flowOf(null)

        override suspend fun create(draft: TransactionDraft): Outcome<Transaction> {
            createdDraft = draft
            return createResult
        }

        override suspend fun update(id: String, draft: TransactionDraft): Outcome<Transaction> =
            createResult

        override suspend fun softDelete(id: String): Outcome<Unit> = Outcome.Success(Unit)
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
        val food = Category(
            id = "food",
            name = "Food & Drinks",
            kind = CategoryKind.EXPENSE,
            parentId = null,
            colorArgb = 0,
            iconKey = "food",
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
        val saved = Transaction(
            id = "tx-1",
            accountId = "cash",
            categoryId = "food",
            type = TransactionType.EXPENSE,
            amountMinor = 1_850L,
            currencyCode = "USD",
            occurredAt = 1L,
            note = null,
            transferGroupId = null,
            recurringRuleId = null,
        )
    }
}
