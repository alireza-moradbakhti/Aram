package com.aram.feature.transactions.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.aram.core.designsystem.AramTheme
import com.aram.core.domain.repository.OutboxSnapshot
import com.aram.core.money.CurrencyCode
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TransactionsScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private var retries = 0
    private var refreshes = 0

    @Test
    fun `queued changes show a banner that retries the outbox`() {
        setScreen(state(outbox = OutboxSnapshot(pending = 2)))

        compose.onNodeWithText("2 changes waiting to sync", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Sync now").performClick()

        assertEquals(1, retries)
    }

    @Test
    fun `failed changes show a retry action`() {
        setScreen(state(outbox = OutboxSnapshot(pending = 1, failed = 1)))

        compose.onNodeWithText("1 change couldn't sync", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Retry").performClick()

        assertEquals(1, retries)
    }

    @Test
    fun `pending rows carry the queued chip`() {
        setScreen(
            state(
                groups = listOf(
                    DayGroupUi(
                        date = LocalDate.of(2026, 11, 20),
                        totalMinor = 12_000,
                        transactions = listOf(
                            TransactionRowUi(
                                id = "tx-1",
                                title = "Groceries",
                                detail = "Food & Drinks",
                                categoryIconKey = "food",
                                amountMinor = 12_000,
                                fractionDigits = 2,
                                incoming = false,
                                pendingSync = true,
                            ),
                        ),
                    ),
                ),
            ),
        )

        compose.onNodeWithText("Queued").assertExists()
    }

    @Test
    fun `the search icon reveals a field that forwards typing`() {
        var query = ""
        setScreen(state(), onSearchQueryChange = { query = it })

        compose.onNodeWithContentDescription("Search transactions").performClick()
        compose.onNodeWithText("Search notes").assertIsDisplayed()
        compose.onNode(hasSetTextAction()).performTextInput("coffee")

        assertEquals("coffee", query)
    }

    @Test
    fun `results mode hides month navigation and stats`() {
        setScreen(
            state(
                groups = listOf(
                    DayGroupUi(
                        date = LocalDate.of(2026, 11, 20),
                        totalMinor = -12_000,
                        transactions = listOf(
                            TransactionRowUi(
                                id = "tx-1",
                                title = "Coffee",
                                detail = "Food & Drinks",
                                categoryIconKey = "food",
                                amountMinor = -12_000,
                                fractionDigits = 2,
                                incoming = false,
                            ),
                        ),
                    ),
                ),
            ).copy(isFiltering = true, query = "coffee", entryCount = 1),
        )

        compose.onNodeWithText("1 result").assertIsDisplayed()
        compose.onNodeWithContentDescription("Previous month").assertDoesNotExist()
        compose.onNodeWithText("SPENT").assertDoesNotExist()
    }

    @Test
    fun `applied filters render as chips`() {
        setScreen(
            state().copy(
                scope = TransactionScope.EXPENSES,
                filters = TransactionFiltersUi(
                    categoryId = "food",
                    dateRange = DateRangePreset.LAST_30_DAYS,
                ),
                categories = listOf(CategoryOptionUi("food", "Food & Drinks", "food")),
                isFiltering = true,
                query = "coffee",
            ),
        )

        compose.onNodeWithText("Food & Drinks").assertExists()
        compose.onNodeWithText("Last 30 days").assertExists()
        compose.onNodeWithText("Clear all").assertExists()
    }

    @Test
    fun `no matches card names the query and clears everything`() {
        var clears = 0
        setScreen(
            state().copy(
                isFiltering = true,
                noMatches = true,
                query = "coffee",
            ),
            onClearFilters = { clears++ },
        )

        compose.onNodeWithText("No matches for \u201Ccoffee\u201D").assertIsDisplayed()
        compose.onNodeWithText("Clear filters").performClick()

        assertEquals(1, clears)
    }

    private fun state(
        outbox: OutboxSnapshot = OutboxSnapshot(),
        groups: List<DayGroupUi> = emptyList(),
    ) = TransactionsUiState(
        month = YearMonth.of(2026, 11),
        today = LocalDate.of(2026, 11, 22),
        loading = false,
        currency = CurrencyCode.USD,
        groups = groups,
        entryCount = groups.sumOf { it.transactions.size },
        outbox = outbox,
    )

    private fun setScreen(
        state: TransactionsUiState,
        onSearchQueryChange: (String) -> Unit = {},
        onClearFilters: () -> Unit = {},
    ) {
        compose.setContent {
            AramTheme(darkTheme = true) {
                TransactionsScreen(
                    state = state,
                    onPreviousMonth = {},
                    onNextMonth = {},
                    onScopeChange = {},
                    onAddTransaction = {},
                    onRefresh = { refreshes++ },
                    onRetryQueued = { retries++ },
                    onSearchQueryChange = onSearchQueryChange,
                    onClearFilters = onClearFilters,
                )
            }
        }
    }
}
