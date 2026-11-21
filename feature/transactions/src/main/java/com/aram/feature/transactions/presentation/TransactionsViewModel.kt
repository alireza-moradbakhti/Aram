package com.aram.feature.transactions.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aram.core.domain.repository.LedgerDirectory
import com.aram.core.domain.repository.LedgerSync
import com.aram.core.domain.repository.TransactionRepository
import com.aram.core.model.Account
import com.aram.core.model.Category
import com.aram.core.model.SyncState
import com.aram.core.model.Transaction
import com.aram.core.model.TransactionType
import com.aram.core.money.CurrencyCode
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class TransactionScope { ALL, EXPENSES, INCOME }

data class TransactionRowUi(
    val id: String,
    val title: String,
    val detail: String,
    val categoryIconKey: String?,
    val amountMinor: Long,
    val fractionDigits: Int,
    val incoming: Boolean,
)

data class DayGroupUi(
    val date: LocalDate,
    val totalMinor: Long,
    val transactions: List<TransactionRowUi>,
)

data class TransactionsUiState(
    val month: YearMonth,
    val today: LocalDate,
    val scope: TransactionScope = TransactionScope.ALL,
    val loading: Boolean = true,
    val currency: CurrencyCode = CurrencyCode.USD,
    val groups: List<DayGroupUi> = emptyList(),
    val entryCount: Int = 0,
    val spentMinor: Long = 0,
    val incomeMinor: Long = 0,
    val incomeCount: Int = 0,
    val previousSpentMinor: Long? = null,
    val sync: SyncState = SyncState(),
) {

    val isEmpty: Boolean get() = !loading && groups.isEmpty()
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val repository: TransactionRepository,
    private val directory: LedgerDirectory,
    private val ledgerSync: LedgerSync,
    private val clock: Clock,
) : ViewModel() {

    private val month = MutableStateFlow(YearMonth.now(clock))
    private val scope = MutableStateFlow(TransactionScope.ALL)

    private val _uiState = MutableStateFlow(
        TransactionsUiState(month = YearMonth.now(clock), today = LocalDate.now(clock)),
    )
    val uiState: StateFlow<TransactionsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(month, scope) { selectedMonth, selectedScope ->
                selectedMonth to selectedScope
            }.flatMapLatest { (selectedMonth, selectedScope) ->
                observeMonth(selectedMonth, selectedScope)
            }.combine(ledgerSync.state) { state, sync ->
                state.copy(sync = sync)
            }.collect { state -> _uiState.value = state }
        }
        viewModelScope.launch {
            month.collect { selectedMonth -> ledgerSync.refresh(selectedMonth) }
        }
    }

    /** Pull-to-refresh and the retry action on the sync banner. */
    fun onRefresh() {
        viewModelScope.launch {
            ledgerSync.refresh(month.value, force = true)
        }
    }

    fun onPreviousMonth() {
        month.update { it.minusMonths(1) }
    }

    fun onNextMonth() {
        month.update { it.plusMonths(1) }
    }

    fun onScopeChange(scope: TransactionScope) {
        this.scope.update { scope }
    }

    private fun observeMonth(month: YearMonth, scope: TransactionScope): Flow<TransactionsUiState> {
        val start = startOfMonth(month)
        val end = startOfMonth(month.plusMonths(1))
        val previousStart = startOfMonth(month.minusMonths(1))
        return combine(
            repository.observeTransactions(start, end),
            repository.observeTransactions(previousStart, start),
            directory.observeAccounts(),
            directory.observeCategories(),
        ) { rows, previousRows, accounts, categories ->
            buildState(month, scope, rows, previousRows, accounts, categories)
        }
    }

    private fun buildState(
        month: YearMonth,
        scope: TransactionScope,
        rows: List<Transaction>,
        previousRows: List<Transaction>,
        accounts: List<Account>,
        categories: List<Category>,
    ): TransactionsUiState {
        val accountsById = accounts.associateBy(Account::id)
        val categoriesById = categories.associateBy(Category::id)
        val currency = displayCurrency(rows, accounts)
        val fractionDigits = currency.fractionDigits

        val groups = rows
            .filter { scope.accepts(it.type) }
            .sortedByDescending(Transaction::occurredAt)
            .groupBy { it.occurredAt.toLocalDate() }
            .toSortedMap(compareByDescending { it })
            .map { (date, dayRows) ->
                val outflow = dayRows
                    .filter { it.type != TransactionType.INCOME }
                    .sumOf(Transaction::amountMinor)
                val inflow = dayRows
                    .filter { it.type == TransactionType.INCOME }
                    .sumOf(Transaction::amountMinor)
                DayGroupUi(
                    date = date,
                    totalMinor = if (outflow > 0) -outflow else inflow,
                    transactions = dayRows.map { it.toRow(accountsById, categoriesById, fractionDigits) },
                )
            }

        val inDisplayCurrency = rows.filter { it.currencyOrNull() == currency }
        val spent = inDisplayCurrency
            .filter { it.type != TransactionType.INCOME }
            .sumOf(Transaction::amountMinor)
        val income = inDisplayCurrency
            .filter { it.type == TransactionType.INCOME }
            .sumOf(Transaction::amountMinor)
        val previousSpent = previousRows
            .filter { it.currencyOrNull() == currency && it.type != TransactionType.INCOME }
            .sumOf(Transaction::amountMinor)

        return TransactionsUiState(
            month = month,
            today = LocalDate.now(clock),
            scope = scope,
            loading = false,
            currency = currency,
            groups = groups,
            entryCount = rows.size,
            spentMinor = spent,
            incomeMinor = income,
            incomeCount = rows.count { it.type == TransactionType.INCOME },
            previousSpentMinor = previousSpent.takeIf { it > 0 },
        )
    }

    private fun displayCurrency(rows: List<Transaction>, accounts: List<Account>): CurrencyCode {
        val dominant = rows
            .mapNotNull { it.currencyOrNull() }
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key
        return dominant
            ?: accounts.firstNotNullOfOrNull { account -> account.currencyOrNull() }
            ?: CurrencyCode.USD
    }

    private fun Transaction.currencyOrNull(): CurrencyCode? =
        CurrencyCode.entries.firstOrNull { it.isoCode.equals(currencyCode, ignoreCase = true) }

    private fun Account.currencyOrNull(): CurrencyCode? =
        CurrencyCode.entries.firstOrNull { it.isoCode.equals(currencyCode, ignoreCase = true) }

    private fun Transaction.toRow(
        accounts: Map<String, Account>,
        categories: Map<String, Category>,
        fractionDigits: Int,
    ): TransactionRowUi {
        val category = categoryId?.let(categories::get)
        val account = accounts[accountId]
        val incoming = type == TransactionType.INCOME
        val categoryLabel = category?.name ?: type.label()
        val title = note?.takeIf { it.isNotBlank() } ?: categoryLabel
        val detail = listOfNotNull(
            categoryLabel.takeIf { it != title },
            TIME_FORMAT.format(Instant.ofEpochMilli(occurredAt).atZone(clock.zone)),
            account?.name,
        ).joinToString(" · ")
        return TransactionRowUi(
            id = id,
            title = title,
            detail = detail,
            categoryIconKey = category?.iconKey,
            amountMinor = if (incoming) amountMinor else -amountMinor,
            fractionDigits = fractionDigits,
            incoming = incoming,
        )
    }

    private fun Long.toLocalDate(): LocalDate =
        Instant.ofEpochMilli(this).atZone(clock.zone).toLocalDate()

    private fun startOfMonth(month: YearMonth): Long =
        month.atDay(1).atStartOfDay(clock.zone).toInstant().toEpochMilli()

    private fun TransactionScope.accepts(type: TransactionType): Boolean = when (this) {
        TransactionScope.ALL -> true
        TransactionScope.EXPENSES -> type != TransactionType.INCOME
        TransactionScope.INCOME -> type == TransactionType.INCOME
    }

    private companion object {
        val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
    }
}
