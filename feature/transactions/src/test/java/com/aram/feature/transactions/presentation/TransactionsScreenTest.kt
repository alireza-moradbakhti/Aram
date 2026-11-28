package com.aram.feature.transactions.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
    fun `tapping a row opens that transaction`() {
        var openedId: String? = null
        compose.setContent {
            AramTheme(darkTheme = true) {
                TransactionsScreen(
                    state = state(
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
                                        pendingSync = false,
                                    ),
                                ),
                            ),
                        ),
                    ),
                    onPreviousMonth = {},
                    onNextMonth = {},
                    onScopeChange = {},
                    onAddTransaction = {},
                    onTransactionClick = { openedId = it },
                    onRefresh = {},
                    onRetryQueued = {},
                )
            }
        }

        compose.onNodeWithText("Groceries").performClick()

        assertEquals("tx-1", openedId)
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

    private fun setScreen(state: TransactionsUiState) {
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
                )
            }
        }
    }
}
