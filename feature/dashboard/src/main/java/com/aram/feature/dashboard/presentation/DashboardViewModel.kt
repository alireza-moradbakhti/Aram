package com.aram.feature.dashboard.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aram.core.domain.repository.LedgerDirectory
import com.aram.core.domain.repository.TransactionRepository
import com.aram.core.model.Account
import com.aram.core.model.Category
import com.aram.core.model.Transaction
import com.aram.core.model.TransactionType
import com.aram.core.money.CurrencyCode
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class RecentTransactionUi(
    val id: String,
    val title: String,
    val meta: String,
    val categoryIconKey: String?,
    /** Signed for display: expenses and transfers are negative, income positive. */
    val amountMinor: Long,
    val fractionDigits: Int,
    val incoming: Boolean,
)

data class DashboardUiState(
    val isLoading: Boolean = true,
    val recent: List<RecentTransactionUi> = emptyList(),
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: TransactionRepository,
    private val directory: LedgerDirectory,
    private val clock: Clock,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        // Only the recent slice is observed here; the full ledger belongs to
        // the Activity tab (AR-018 acceptance: no duplicate full-list loading).
        viewModelScope.launch {
            combine(
                repository.observeRecent(RECENT_LIMIT),
                directory.observeCategories(),
                directory.observeAccounts(),
            ) { rows, categories, accounts ->
                val categoriesById = categories.associateBy(Category::id)
                val accountsById = accounts.associateBy(Account::id)
                val today = LocalDate.now(clock)
                DashboardUiState(
                    isLoading = false,
                    recent = rows.map { it.toRecent(categoriesById, accountsById, today) },
                )
            }.collect { _uiState.value = it }
        }
    }

    private fun Transaction.toRecent(
        categories: Map<String, Category>,
        accounts: Map<String, Account>,
        today: LocalDate,
    ): RecentTransactionUi {
        val category = categoryId?.let(categories::get)
        val account = accounts[accountId]
        val incoming = type == TransactionType.INCOME
        val categoryLabel = category?.name ?: type.defaultLabel()
        val title = note?.takeIf { it.isNotBlank() } ?: categoryLabel
        val occurred = Instant.ofEpochMilli(occurredAt).atZone(clock.zone)
        val dateLabel = if (occurred.toLocalDate() == today) {
            TIME_FORMAT.format(occurred)
        } else {
            DATE_FORMAT.format(occurred)
        }
        val meta = listOfNotNull(
            categoryLabel.takeIf { it != title },
            dateLabel,
            account?.name?.takeIf { type == TransactionType.TRANSFER },
        ).joinToString(" · ")
        return RecentTransactionUi(
            id = id,
            title = title,
            meta = meta,
            categoryIconKey = category?.iconKey,
            amountMinor = if (incoming) amountMinor else -amountMinor,
            fractionDigits = CurrencyCode.entries
                .firstOrNull { it.isoCode.equals(currencyCode, ignoreCase = true) }
                ?.fractionDigits
                ?: 0,
            incoming = incoming,
        )
    }

    private fun TransactionType.defaultLabel(): String = when (this) {
        TransactionType.INCOME -> "Income"
        TransactionType.EXPENSE -> "Expense"
        TransactionType.TRANSFER -> "Transfer"
    }

    private companion object {
        const val RECENT_LIMIT = 5
        val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
        val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.US)
    }
}