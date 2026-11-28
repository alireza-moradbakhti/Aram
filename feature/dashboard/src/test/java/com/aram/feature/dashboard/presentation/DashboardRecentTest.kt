package com.aram.feature.dashboard.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.aram.core.designsystem.AramTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w390dp-h1000dp-xhdpi")
class DashboardRecentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val rows = listOf(
        RecentTransactionUi(
            id = "t1",
            title = "Café Nima",
            meta = "Groceries · 09:12",
            categoryIconKey = "groceries",
            amountMinor = -180_000,
            fractionDigits = 0,
            incoming = false,
        ),
        RecentTransactionUi(
            id = "t2",
            title = "Transfer",
            meta = "10:00 · Main account",
            categoryIconKey = null,
            amountMinor = -2_000_000,
            fractionDigits = 0,
            incoming = false,
        ),
    )

    @Test
    fun `renders the recent rows supplied by state`() {
        composeRule.setContent {
            Host(recent = rows)
        }

        composeRule.onNodeWithText("Café Nima").performScrollTo().assertExists()
        composeRule.onNodeWithText("Groceries · 09:12").assertExists()
        composeRule.onNodeWithText("Transfer").assertExists()
        composeRule.onNodeWithText("10:00 · Main account").assertExists()
        composeRule.onNodeWithText("\u2212180,000").assertExists()
    }

    @Test
    fun `tapping a row opens the ledger`() {
        var opened = 0
        composeRule.setContent {
            Host(recent = rows, onOpenLedger = { opened++ })
        }

        composeRule.onNodeWithText("Café Nima").performScrollTo().performClick()

        assertEquals(1, opened)
    }

    @Test
    fun `see all opens the ledger`() {
        var opened = 0
        composeRule.setContent {
            Host(recent = rows, onOpenLedger = { opened++ })
        }

        composeRule.onAllNodesWithText("See all")[0].performScrollTo().performClick()

        assertEquals(1, opened)
    }

    @Test
    fun `an empty ledger shows the add-expense call to action`() {
        var added = 0
        composeRule.setContent {
            Host(recent = emptyList(), onAddTransaction = { added++ })
        }

        composeRule.onNodeWithText("No transactions yet").performScrollTo().assertExists()
        composeRule.onNodeWithText("Add expense").performScrollTo().performClick()

        assertEquals(1, added)
        composeRule.onAllNodesWithText("Groceries · 09:12").assertCountEquals(0)
    }

    @androidx.compose.runtime.Composable
    private fun Host(
        recent: List<RecentTransactionUi>,
        onOpenLedger: () -> Unit = {},
        onAddTransaction: () -> Unit = {},
    ) {
        AramTheme(darkTheme = true) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
            ) {
                DashboardScreen(
                    displayName = "Ava Bennett",
                    isLoading = false,
                    recent = recent,
                    onOpenLedger = onOpenLedger,
                    onAddTransaction = onAddTransaction,
                )
            }
        }
    }
}