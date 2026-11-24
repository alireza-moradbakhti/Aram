package com.aram.feature.transactions.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aram.core.designsystem.state.SubmitState
import com.aram.core.domain.repository.LedgerDirectory
import com.aram.core.domain.transaction.TransactionDraft
import com.aram.core.domain.transaction.TransactionField
import com.aram.core.domain.transaction.TransactionWriteResult
import com.aram.core.domain.usecase.CreateTransaction
import com.aram.core.model.Account
import com.aram.core.model.Category
import com.aram.core.model.CategoryKind
import com.aram.core.model.TransactionType
import com.aram.core.money.AmountParseResult
import com.aram.core.money.AmountParser
import com.aram.core.money.CurrencyCode
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface AmountInputError {
    data object Empty : AmountInputError

    data object Zero : AmountInputError

    data object Invalid : AmountInputError

    data class TooManyFractionDigits(val max: Int) : AmountInputError
}

data class AddTransactionUiState(
    val type: TransactionType = TransactionType.EXPENSE,
    val amountInput: String = "",
    val amountError: AmountInputError? = null,
    val currency: CurrencyCode = CurrencyCode.USD,
    val accounts: List<Account> = emptyList(),
    val selectedAccountId: String? = null,
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: String? = null,
    val occurredOn: LocalDate = LocalDate.now(),
    val note: String = "",
    val optionsLoaded: Boolean = false,
    val invalidFields: Set<TransactionField> = emptySet(),
    val submitState: SubmitState = SubmitState.Idle,
    val saved: Boolean = false,
) {

    val selectedAccount: Account?
        get() = accounts.firstOrNull { it.id == selectedAccountId }

    val visibleCategories: List<Category>
        get() = categories.filter { it.kind == type.categoryKind }

    val canSave: Boolean
        get() = optionsLoaded && accounts.isNotEmpty() &&
            submitState !is SubmitState.Submitting && !saved
}

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val createTransaction: CreateTransaction,
    directory: LedgerDirectory,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddTransactionUiState())
    val uiState: StateFlow<AddTransactionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                directory.observeAccounts(),
                directory.observeCategories(),
            ) { accounts, categories -> accounts to categories }
                .collect { (accounts, categories) -> onDirectoryChanged(accounts, categories) }
        }
    }

    fun onTypeChange(type: TransactionType) {
        if (type == TransactionType.TRANSFER) return
        _uiState.update { state ->
            if (state.type == type) return@update state
            val matching = state.categories.filter { it.kind == type.categoryKind }
            state.copy(
                type = type,
                selectedCategoryId = matching.firstOrNull()?.id,
                invalidFields = state.invalidFields - TransactionField.CATEGORY,
                submitState = SubmitState.Idle,
            )
        }
    }

    fun onAmountChange(input: String) {
        _uiState.update {
            it.copy(
                amountInput = input.take(MAX_AMOUNT_LENGTH),
                amountError = null,
                invalidFields = it.invalidFields - TransactionField.AMOUNT,
            )
        }
    }

    fun onAccountSelected(accountId: String) {
        _uiState.update { state ->
            val account = state.accounts.firstOrNull { it.id == accountId } ?: return@update state
            state.copy(
                selectedAccountId = account.id,
                currency = currencyOf(account),
                invalidFields = state.invalidFields - TransactionField.ACCOUNT,
                submitState = SubmitState.Idle,
            )
        }
    }

    fun onCategorySelected(categoryId: String) {
        _uiState.update { state ->
            if (state.categories.none { it.id == categoryId }) return@update state
            state.copy(
                selectedCategoryId = categoryId,
                invalidFields = state.invalidFields - TransactionField.CATEGORY,
                submitState = SubmitState.Idle,
            )
        }
    }

    fun onDateSelected(date: LocalDate) {
        _uiState.update { it.copy(occurredOn = date) }
    }

    fun onNoteChange(note: String) {
        _uiState.update { it.copy(note = note.take(MAX_NOTE_LENGTH)) }
    }

    fun onSave() {
        val state = _uiState.value
        if (state.submitState is SubmitState.Submitting || state.saved) return

        val parsed = AmountParser.parse(state.amountInput, state.currency)
        val amountError = parsed.toInputError()
        val accountId = state.selectedAccountId
        val categoryId = state.selectedCategoryId
        if (parsed !is AmountParseResult.Valid || accountId == null || categoryId == null) {
            _uiState.update {
                it.copy(
                    amountError = amountError,
                    invalidFields = buildSet {
                        if (amountError != null) add(TransactionField.AMOUNT)
                        if (accountId == null) add(TransactionField.ACCOUNT)
                        if (categoryId == null) add(TransactionField.CATEGORY)
                    },
                )
            }
            return
        }

        val draft = TransactionDraft(
            accountId = accountId,
            categoryId = categoryId,
            type = state.type,
            amountMinor = parsed.money.amountMinor,
            currencyCode = state.currency.isoCode,
            occurredAt = state.occurredOn
                .atTime(LocalTime.now())
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli(),
            note = state.note.takeIf { it.isNotBlank() },
        )

        _uiState.update { it.copy(submitState = SubmitState.Submitting) }
        viewModelScope.launch {
            when (val result = createTransaction(draft)) {
                is TransactionWriteResult.Success -> _uiState.update {
                    it.copy(submitState = SubmitState.Idle, saved = true)
                }

                is TransactionWriteResult.Invalid -> _uiState.update {
                    it.copy(
                        submitState = SubmitState.Idle,
                        invalidFields = result.problems.keys,
                    )
                }

                is TransactionWriteResult.Failed -> _uiState.update {
                    it.copy(submitState = SubmitState.Failed(result.error))
                }
            }
        }
    }

    private fun onDirectoryChanged(accounts: List<Account>, categories: List<Category>) {
        _uiState.update { state ->
            val account = accounts.firstOrNull { it.id == state.selectedAccountId }
                ?: accounts.firstOrNull()
            val matching = categories.filter { it.kind == state.type.categoryKind }
            val category = matching.firstOrNull { it.id == state.selectedCategoryId }
                ?: matching.firstOrNull()
            state.copy(
                accounts = accounts,
                selectedAccountId = account?.id,
                categories = categories,
                selectedCategoryId = category?.id,
                currency = account?.let(::currencyOf) ?: state.currency,
                optionsLoaded = true,
            )
        }
    }

    private fun currencyOf(account: Account): CurrencyCode =
        runCatching { CurrencyCode.fromIsoCode(account.currencyCode) }
            .getOrDefault(CurrencyCode.USD)

    private fun AmountParseResult.toInputError(): AmountInputError? = when (this) {
        is AmountParseResult.Valid -> null
        AmountParseResult.Empty -> AmountInputError.Empty
        AmountParseResult.Zero -> AmountInputError.Zero
        AmountParseResult.Invalid -> AmountInputError.Invalid
        is AmountParseResult.TooManyFractionDigits -> AmountInputError.TooManyFractionDigits(max)
    }

    private companion object {
        const val MAX_AMOUNT_LENGTH = 15
        const val MAX_NOTE_LENGTH = 80
    }
}

private val TransactionType.categoryKind: CategoryKind?
    get() = when (this) {
        TransactionType.INCOME -> CategoryKind.INCOME
        TransactionType.EXPENSE -> CategoryKind.EXPENSE
        TransactionType.TRANSFER -> null
    }
