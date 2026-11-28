package com.aram.feature.dashboard.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aram.core.domain.repository.LedgerDirectory
import com.aram.core.domain.repository.TransactionRepository
import com.aram.core.domain.safetospend.SafeToSpend
import com.aram.core.domain.safetospend.SafeToSpendCalculator
import com.aram.core.model.Account
import com.aram.core.model.Transaction
import com.aram.core.model.TransactionType
import com.aram.core.money.CurrencyCode
import com.aram.core.money.Money
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardUiState(
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
    val month: YearMonth? = null,
    val safeToSpend: SafeToSpend? = null,
    val spent: Money? = null,
    val saved: Money? = null,
    val previousSpent: Money? = null,
    val previousSaved: Money? = null,
    val isEmpty: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: TransactionRepository,
    private val directory: LedgerDirectory,
    private val clock: Clock,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val reload = MutableStateFlow(0)
    private val calculator = SafeToSpendCalculator(clock)

    init {
        viewModelScope.launch {
            reload.flatMapLatest { load() }.collect { state -> _uiState.value = state }
        }
    }

    fun retry() {
        _uiState.update { it.copy(isLoading = true, hasError = false) }
        reload.value += 1
    }

    private fun load(): Flow<DashboardUiState> {
        val today = LocalDate.now(clock)
        val month = YearMonth.from(today)
        val zone = clock.zone
        fun startOf(month: YearMonth) = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()

        return combine(
            repository.observeTransactions(startOf(month), startOf(month.plusMonths(1))),
            repository.observeTransactions(startOf(month.minusMonths(1)), startOf(month)),
            directory.observeAccounts(),
        ) { rows, previousRows, accounts ->
            buildState(month, rows, previousRows, accounts)
        }
            .onStart { emit(DashboardUiState(isLoading = true, month = month)) }
            .catch { emit(DashboardUiState(isLoading = false, hasError = true, month = month)) }
    }

    private fun buildState(
        month: YearMonth,
        rows: List<Transaction>,
        previousRows: List<Transaction>,
        accounts: List<Account>,
    ): DashboardUiState {
        val currency = displayCurrency(rows + previousRows, accounts)
        val current = rows.inCurrency(currency)
        val previous = previousRows.inCurrency(currency)

        val income = current.incomeTotal(currency)
        val spent = current.expenseTotal(currency)
        val previousIncome = previous.incomeTotal(currency)
        val previousSpent = previous.expenseTotal(currency)

        return DashboardUiState(
            isLoading = false,
            month = month,
            safeToSpend = calculator.calculate(
                month = month,
                income = income,
                spent = spent,
                plannedBudgets = Money.zero(currency),
                upcomingBills = Money.zero(currency),
            ),
            spent = spent,
            saved = income - spent,
            previousSpent = previousSpent.takeIf { it.amountMinor > 0 },
            previousSaved = (previousIncome - previousSpent).takeIf { previous.isNotEmpty() },
            isEmpty = current.isEmpty(),
        )
    }

    private fun List<Transaction>.inCurrency(currency: CurrencyCode) =
        filter { it.isoCurrency() == currency && it.type != TransactionType.TRANSFER }

    private fun List<Transaction>.incomeTotal(currency: CurrencyCode) =
        Money(filter { it.type == TransactionType.INCOME }.sumOf(Transaction::amountMinor), currency)

    private fun List<Transaction>.expenseTotal(currency: CurrencyCode) =
        Money(filter { it.type == TransactionType.EXPENSE }.sumOf(Transaction::amountMinor), currency)

    private fun Transaction.isoCurrency(): CurrencyCode? =
        CurrencyCode.entries.firstOrNull { it.isoCode.equals(currencyCode, ignoreCase = true) }

    private fun displayCurrency(rows: List<Transaction>, accounts: List<Account>): CurrencyCode {
        val dominant = rows
            .mapNotNull { it.isoCurrency() }
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key
        return dominant
            ?: accounts.firstNotNullOfOrNull { account ->
                CurrencyCode.entries.firstOrNull {
                    it.isoCode.equals(account.currencyCode, ignoreCase = true)
                }
            }
            ?: CurrencyCode.IRR
    }
}