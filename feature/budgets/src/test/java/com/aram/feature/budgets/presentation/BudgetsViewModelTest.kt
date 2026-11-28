package com.aram.feature.budgets.presentation

import com.aram.core.domain.budget.BudgetDraft
import com.aram.core.domain.budget.BudgetProgress
import com.aram.core.domain.repository.BudgetRepository
import com.aram.core.model.Budget
import com.aram.core.model.Category
import com.aram.core.model.CategoryKind
import com.aram.core.model.Outcome
import java.time.Clock
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
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
class BudgetsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val clock = Clock.fixed(Instant.parse("2026-11-12T09:00:00Z"), ZoneOffset.UTC)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `totals and rows come from the repository for the current month`() = runTest(dispatcher) {
        val repository = FakeBudgetRepository(
            progress = listOf(
                progress("groceries", "Groceries", limit = 6_000_000, spent = 4_120_000),
                progress("dining", "Dining out", limit = 2_000_000, spent = 1_760_000),
            ),
        )
        val viewModel = BudgetsViewModel(repository, clock)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.loading)
        assertEquals(YearMonth.of(2026, 11), state.month)
        assertTrue(state.isCurrentMonth)
        assertEquals(8_000_000L, state.totalLimitMinor)
        assertEquals(5_880_000L, state.totalSpentMinor)
        assertEquals(2_120_000L, state.totalRemainingMinor)
        assertEquals(73, state.totalPercentUsed)
        assertEquals(2, state.rows.size)
        assertEquals("IRR", state.currencyCode)
        assertEquals(19, state.daysLeft)
        assertEquals(2_120_000L / 19, state.perDayMinor)
        assertEquals(BudgetStatus.ON_TRACK, state.status)
    }

    @Test
    fun `row tones use the 80 and 100 percent thresholds`() = runTest(dispatcher) {
        val repository = FakeBudgetRepository(
            progress = listOf(
                progress("a", "Alpha", limit = 1_000, spent = 799),
                progress("b", "Bravo", limit = 1_000, spent = 800),
                progress("c", "Charlie", limit = 1_000, spent = 1_010),
            ),
        )
        val viewModel = BudgetsViewModel(repository, clock)

        advanceUntilIdle()

        val tones = viewModel.uiState.value.rows.map { it.name to it.tone }
        assertEquals(
            listOf(
                "Alpha" to BudgetRowTone.OK,
                "Bravo" to BudgetRowTone.NEAR_LIMIT,
                "Charlie" to BudgetRowTone.OVER,
            ),
            tones,
        )
        assertEquals(10L, viewModel.uiState.value.rows.last().overByMinor)
        assertEquals(BudgetStatus.NEAR_LIMIT, viewModel.uiState.value.status)
    }

    @Test
    fun `headline status turns over only when the month total passes its limit`() = runTest(dispatcher) {
        val viewModel = BudgetsViewModel(
            FakeBudgetRepository(progress = listOf(progress("a", "Alpha", limit = 1_000, spent = 1_001))),
            clock,
        )

        advanceUntilIdle()

        assertEquals(BudgetStatus.OVER, viewModel.uiState.value.status)
    }

    @Test
    fun `empty month is marked empty with zero totals`() = runTest(dispatcher) {
        val viewModel = BudgetsViewModel(FakeBudgetRepository(), clock)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isEmpty)
        assertEquals(0L, state.totalLimitMinor)
        assertEquals(BudgetStatus.ON_TRACK, state.status)
    }

    @Test
    fun `month navigation observes the new month and the shortcut returns`() = runTest(dispatcher) {
        val repository = FakeBudgetRepository()
        val viewModel = BudgetsViewModel(repository, clock)

        advanceUntilIdle()
        viewModel.onPreviousMonth()
        advanceUntilIdle()

        assertEquals(YearMonth.of(2026, 10), viewModel.uiState.value.month)
        assertFalse(viewModel.uiState.value.isCurrentMonth)
        assertEquals(0, viewModel.uiState.value.daysLeft)
        assertEquals(listOf(YearMonth.of(2026, 11), YearMonth.of(2026, 10)), repository.months)

        viewModel.onCurrentMonth()
        advanceUntilIdle()

        assertEquals(YearMonth.of(2026, 11), viewModel.uiState.value.month)
        assertTrue(viewModel.uiState.value.isCurrentMonth)
    }

    @Test
    fun `future month counts its full length while past month has none left`() = runTest(dispatcher) {
        val viewModel = BudgetsViewModel(FakeBudgetRepository(), clock)

        advanceUntilIdle()
        viewModel.onNextMonth()
        advanceUntilIdle()

        assertEquals(31, viewModel.uiState.value.daysLeft)
        assertEquals(YearMonth.of(2026, 12), viewModel.uiState.value.month)
    }

    @Test
    fun `totals ignore a minority currency but rows keep their own`() = runTest(dispatcher) {
        val repository = FakeBudgetRepository(
            progress = listOf(
                progress("a", "Groceries", limit = 6_000_000, spent = 4_000_000),
                progress("b", "Dining out", limit = 2_000_000, spent = 1_000_000),
                progress("c", "Travel", limit = 50_000, spent = 10_000, currency = "USD"),
            ),
        )
        val viewModel = BudgetsViewModel(repository, clock)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("IRR", state.currencyCode)
        assertEquals(8_000_000L, state.totalLimitMinor)
        assertEquals(5_000_000L, state.totalSpentMinor)
        val usd = state.rows.first { it.currencyCode == "USD" }
        assertEquals(2, usd.fractionDigits)
    }
}

private fun progress(
    slug: String,
    name: String,
    limit: Long,
    spent: Long,
    currency: String = "IRR",
): BudgetProgress = BudgetProgress(
    budget = Budget(
        id = "budget-$slug",
        categoryId = "category-$slug",
        periodStartEpochDay = 0,
        limitMinor = limit,
        currencyCode = currency,
    ),
    category = Category(
        id = "category-$slug",
        name = name,
        kind = CategoryKind.EXPENSE,
        parentId = null,
        colorArgb = 0,
        iconKey = slug,
        archived = false,
    ),
    spentMinor = spent,
)

internal class FakeBudgetRepository(
    private var progress: List<BudgetProgress> = emptyList(),
) : BudgetRepository {

    val months = mutableListOf<YearMonth>()

    override fun observeProgress(month: YearMonth): Flow<List<BudgetProgress>> {
        months += month
        return flowOf(progress)
    }

    override suspend fun upsert(draft: BudgetDraft): Outcome<Budget> =
        error("upsert is not part of the screen tests")

    override suspend fun delete(id: String): Outcome<Unit> =
        error("delete is not part of the screen tests")
}