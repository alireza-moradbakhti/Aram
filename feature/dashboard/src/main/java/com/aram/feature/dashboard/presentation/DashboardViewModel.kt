package com.aram.feature.dashboard.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aram.core.domain.repository.LedgerDirectory
import com.aram.core.domain.repository.TransactionRepository
import com.aram.core.model.Category
import com.aram.core.model.Transaction
import com.aram.core.model.TransactionType
import com.aram.core.money.CurrencyCode
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardUiState(
    val isLoading: Boolean = true,
    val donut: DonutUiState = DonutUiState(),
)

data class DonutUiState(
    val monthLabel: String = "",
    val totalMinor: Long = 0L,
    val fractionDigits: Int = 0,
    val slices: List<DonutSlice> = emptyList(),
) {
    val isEmpty: Boolean get() = slices.isEmpty()
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: TransactionRepository,
    private val directory: LedgerDirectory,
    private val clock: Clock,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        // Placeholder until the remaining dashboard tiles read the cache-first
        // repositories; the skeleton must come from state, not the screen.
        viewModelScope.launch {
            delay(DEMO_LOAD_MILLIS)
            _uiState.update { it.copy(isLoading = false) }
        }
        viewModelScope.launch { observeMonthSpending() }
    }

    private suspend fun observeMonthSpending() {
        val month = LocalDate.now(clock).withDayOfMonth(1)
        val from = month.atStartOfDay(clock.zone).toInstant().toEpochMilli()
        val to = month.plusMonths(1).atStartOfDay(clock.zone).toInstant().toEpochMilli()

        combine(
            repository.observeTransactions(from, to),
            directory.observeCategories(),
        ) { rows, categories ->
            toDonutState(rows, categories, month)
        }.collect { donut ->
            _uiState.update { it.copy(donut = donut) }
        }
    }

    private fun toDonutState(
        rows: List<Transaction>,
        categories: List<Category>,
        month: LocalDate,
    ): DonutUiState {
        val categoriesById = categories.associateBy(Category::id)
        val spends = rows
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy(Transaction::categoryId)
            .mapNotNull { (categoryId, transactions) ->
                val amount = transactions.sumOf(Transaction::amountMinor)
                if (amount <= 0L) return@mapNotNull null
                val category = categoryId?.let(categoriesById::get)
                CategorySpend(
                    categoryId = categoryId ?: UNCATEGORIZED,
                    label = category?.name ?: UNCATEGORIZED_LABEL,
                    colorArgb = category?.colorArgb ?: 0,
                    amountMinor = amount,
                )
            }
        return DonutUiState(
            monthLabel = MONTH_FORMAT.format(month),
            totalMinor = spends.sumOf(CategorySpend::amountMinor),
            fractionDigits = fractionDigits(rows),
            slices = buildDonutSlices(spends),
        )
    }

    private fun fractionDigits(rows: List<Transaction>): Int {
        val code = rows.firstOrNull()?.currencyCode ?: return 0
        return CurrencyCode.entries
            .firstOrNull { it.isoCode.equals(code, ignoreCase = true) }
            ?.fractionDigits
            ?: 0
    }

    private companion object {
        const val DEMO_LOAD_MILLIS = 650L
        const val UNCATEGORIZED = "uncategorized"
        const val UNCATEGORIZED_LABEL = "Other"
        val MONTH_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM", Locale.US)
    }
}