package com.aram.feature.budgets.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aram.core.domain.budget.BudgetProgress
import com.aram.core.domain.repository.BudgetRepository
import com.aram.core.money.CurrencyCode
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class BudgetRowTone { OK, NEAR_LIMIT, OVER }

enum class BudgetStatus { ON_TRACK, NEAR_LIMIT, OVER }

data class BudgetRowUi(
    val id: String,
    val categoryId: String,
    val name: String,
    val iconKey: String,
    val spentMinor: Long,
    val limitMinor: Long,
    val remainingMinor: Long,
    val fraction: Float,
    val percentUsed: Int,
    val overByMinor: Long,
    val currencyCode: String,
    val fractionDigits: Int,
    val tone: BudgetRowTone,
)

data class BudgetsUiState(
    val month: YearMonth,
    val isCurrentMonth: Boolean,
    val loading: Boolean = true,
    val currencyCode: String = DEFAULT_CURRENCY,
    val fractionDigits: Int = 0,
    val totalLimitMinor: Long = 0L,
    val totalSpentMinor: Long = 0L,
    val totalRemainingMinor: Long = 0L,
    val totalFraction: Float = 0f,
    val totalPercentUsed: Int = 0,
    val daysLeft: Int = 0,
    val perDayMinor: Long = 0L,
    val rows: List<BudgetRowUi> = emptyList(),
) {
    val isEmpty: Boolean get() = !loading && rows.isEmpty()

    /** Calm headline status; over-limit rows still carry their own tone. */
    val status: BudgetStatus
        get() = when {
            totalLimitMinor <= 0L -> BudgetStatus.ON_TRACK
            totalSpentMinor > totalLimitMinor -> BudgetStatus.OVER
            totalPercentUsed >= NEAR_LIMIT_PERCENT -> BudgetStatus.NEAR_LIMIT
            else -> BudgetStatus.ON_TRACK
        }

    private companion object {
        const val NEAR_LIMIT_PERCENT = 80
        const val DEFAULT_CURRENCY = "IRR"
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BudgetsViewModel @Inject constructor(
    private val repository: BudgetRepository,
    private val clock: Clock,
) : ViewModel() {

    private val month = MutableStateFlow(YearMonth.now(clock))

    private val _uiState = MutableStateFlow(
        BudgetsUiState(month = YearMonth.now(clock), isCurrentMonth = true),
    )
    val uiState: StateFlow<BudgetsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            month
                .flatMapLatest { selected ->
                    repository.observeProgress(selected).map { rows -> buildState(selected, rows) }
                }
                .collect { state -> _uiState.value = state }
        }
    }

    fun onPreviousMonth() {
        month.update { it.minusMonths(1) }
    }

    fun onNextMonth() {
        month.update { it.plusMonths(1) }
    }

    fun onCurrentMonth() {
        month.value = YearMonth.now(clock)
    }

    private fun buildState(selected: YearMonth, progress: List<BudgetProgress>): BudgetsUiState {
        val today = LocalDate.now(clock)
        val currentMonth = YearMonth.from(today)
        val rows = progress.map { it.toRowUi() }
        val display = rows
            .groupingBy(BudgetRowUi::currencyCode)
            .eachCount()
            .maxByOrNull { it.value }
            ?.key
            ?: DEFAULT_CURRENCY
        val displayRows = rows.filter { it.currencyCode == display }
        val limit = displayRows.sumOf(BudgetRowUi::limitMinor)
        val spent = displayRows.sumOf(BudgetRowUi::spentMinor)
        val remaining = (limit - spent).coerceAtLeast(0L)
        val percentUsed = percentOf(spent, limit)
        return BudgetsUiState(
            month = selected,
            isCurrentMonth = selected == currentMonth,
            loading = false,
            currencyCode = display,
            fractionDigits = fractionDigitsOf(display),
            totalLimitMinor = limit,
            totalSpentMinor = spent,
            totalRemainingMinor = remaining,
            totalFraction = fractionOf(spent, limit),
            totalPercentUsed = percentUsed,
            daysLeft = daysLeft(selected, today),
            perDayMinor = remaining / daysLeft(selected, today).coerceAtLeast(1),
            rows = rows,
        )
    }

    private fun BudgetProgress.toRowUi(): BudgetRowUi {
        val percent = percentOf(spentMinor, limitMinor)
        return BudgetRowUi(
            id = budget.id,
            categoryId = category.id,
            name = category.name,
            iconKey = category.iconKey,
            spentMinor = spentMinor,
            limitMinor = limitMinor,
            remainingMinor = remainingMinor,
            fraction = fraction,
            percentUsed = percent,
            overByMinor = (spentMinor - limitMinor).coerceAtLeast(0L),
            currencyCode = budget.currencyCode,
            fractionDigits = fractionDigitsOf(budget.currencyCode),
            tone = when {
                isOver -> BudgetRowTone.OVER
                percent >= NEAR_LIMIT_PERCENT -> BudgetRowTone.NEAR_LIMIT
                else -> BudgetRowTone.OK
            },
        )
    }

    private fun daysLeft(selected: YearMonth, today: LocalDate): Int = when {
        selected == YearMonth.from(today) -> selected.lengthOfMonth() - today.dayOfMonth + 1
        selected.isAfter(YearMonth.from(today)) -> selected.lengthOfMonth()
        else -> 0
    }

    private fun percentOf(part: Long, whole: Long): Int =
        if (whole <= 0L) 0 else (part * 100 / whole).toInt()

    private fun fractionOf(part: Long, whole: Long): Float =
        if (whole <= 0L) 0f else (part.toDouble() / whole.toDouble()).coerceIn(0.0, 1.0).toFloat()

    private fun fractionDigitsOf(isoCode: String): Int =
        runCatching { CurrencyCode.fromIsoCode(isoCode).fractionDigits }.getOrDefault(0)

    private companion object {
        const val NEAR_LIMIT_PERCENT = 80
        const val DEFAULT_CURRENCY = "IRR"
    }
}