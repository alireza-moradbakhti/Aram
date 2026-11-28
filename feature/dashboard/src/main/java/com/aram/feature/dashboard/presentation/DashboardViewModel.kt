package com.aram.feature.dashboard.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aram.core.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardUiState(
    val isLoading: Boolean = true,
    val trend: TrendUiState = TrendUiState(),
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: TransactionRepository,
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
        viewModelScope.launch { observeTrend() }
    }

    private suspend fun observeTrend() {
        val month = LocalDate.now(clock).withDayOfMonth(1)
        val firstMonth = month.minusMonths((TREND_MONTHS - 1).toLong())
        val from = firstMonth.atStartOfDay(clock.zone).toInstant().toEpochMilli()
        val to = month.plusMonths(1).atStartOfDay(clock.zone).toInstant().toEpochMilli()

        repository.observeTransactions(from, to).collect { rows ->
            _uiState.update {
                it.copy(
                    trend = TrendUiState(
                        buckets = buildMonthlyTrend(
                            rows = rows,
                            endMonth = YearMonth.from(month),
                            zone = clock.zone,
                            months = TREND_MONTHS,
                        ),
                    ),
                )
            }
        }
    }

    private companion object {
        const val DEMO_LOAD_MILLIS = 650L
        const val TREND_MONTHS = 6
    }
}