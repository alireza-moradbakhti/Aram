package com.aram.feature.dashboard.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.aram.core.designsystem.AramTheme
import com.aram.core.domain.safetospend.SafeToSpend
import com.aram.core.domain.safetospend.SafeToSpendBreakdown
import com.aram.core.money.CurrencyCode
import com.aram.core.money.Money
import java.time.YearMonth
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
class SafeToSpendExplainerTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val month = YearMonth.of(2026, 11)

    @Test
    fun `content lists every breakdown line with a spoken value`() {
        val result = safeToSpend(
            income = 45_000_000,
            spent = 18_644_000,
            total = 26_356_000,
            days = 3,
            daily = 8_785_333,
        )
        showSheet(result)

        composeRule.onNodeWithText("How safe-to-spend works").assertIsDisplayed()
        composeRule.onNodeWithText("Income this month", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("Spent so far", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("Planned budgets", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("Upcoming bills", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("Safe to spend", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("Days left", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("Daily amount", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("Overspend adjustment", useUnmergedTree = true).assertDoesNotExist()

        composeRule.onNodeWithContentDescription("Income this month: 45,000,000 IRR")
            .assertExists()
        composeRule.onNodeWithContentDescription("Spent so far: minus 18,644,000 IRR")
            .assertExists()
        composeRule.onNodeWithContentDescription("Safe to spend: 26,356,000 IRR")
            .assertExists()
        composeRule.onNodeWithContentDescription("Days left: 3 days left in November")
            .assertExists()
        composeRule.onNodeWithContentDescription("Daily amount: 8,785,333 IRR a day")
            .assertExists()
        composeRule.onNodeWithText("Keep daily spending at or under 8,785,333 IRR to finish November on track.")
            .assertIsDisplayed()
    }

    @Test
    fun `clamped result shows the adjustment row and explains the zero`() {
        val result = safeToSpend(
            income = 5_000_000,
            spent = 7_000_000,
            adjustment = 2_000_000,
            total = 0,
            days = 10,
            daily = 0,
        )
        showSheet(result)

        composeRule.onNodeWithText("Overspend adjustment", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Overspend adjustment: 2,000,000 IRR")
            .assertExists()
        composeRule.onNodeWithText("pinned at zero", substring = true).assertIsDisplayed()
    }

    @Test
    fun `empty month explains that income is missing`() {
        val result = safeToSpend(
            income = 0,
            spent = 0,
            total = 0,
            days = 30,
            daily = 0,
        )
        showSheet(result)

        composeRule.onNodeWithText("No income is recorded for November yet.", substring = true)
            .assertIsDisplayed()
        composeRule.onAllNodesWithText("0", useUnmergedTree = true).assertCountEquals(5)
    }

    @Test
    fun `advice covers on-track, clamped, and missing income`() {
        val onTrack = explainerAdvice(
            safeToSpend(income = 45_000_000, spent = 18_644_000, total = 26_356_000, days = 3, daily = 8_785_333),
            month,
        )
        assertEquals(
            "Keep daily spending at or under 8,785,333 IRR to finish November on track.",
            onTrack,
        )

        val clamped = explainerAdvice(
            safeToSpend(income = 5_000_000, spent = 7_000_000, adjustment = 2_000_000, total = 0, days = 10, daily = 0),
            month,
        )
        assertEquals(
            "Planned spending is higher than income, so safe-to-spend is pinned at zero. " +
                "Trim a budget or add income to free up room.",
            clamped,
        )

        val noIncome = explainerAdvice(
            safeToSpend(income = 0, spent = 0, total = 0, days = 30, daily = 0),
            month,
        )
        assertEquals("No income is recorded for November yet. Add it to see how much is safe to spend.", noIncome)

        assertEquals(
            "Add income and spending to see how far your money can stretch this month.",
            explainerAdvice(null, month),
        )
    }

    private fun showSheet(result: SafeToSpend) {
        composeRule.setContent {
            AramTheme(darkTheme = false) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                ) {
                    SafeToSpendExplainerContent(
                        result = result,
                        month = month,
                        onDismiss = {},
                    )
                }
            }
        }
    }

    private fun safeToSpend(
        income: Long,
        spent: Long,
        adjustment: Long = 0,
        total: Long,
        days: Int,
        daily: Long,
    ): SafeToSpend {
        val currency = CurrencyCode.IRR
        val breakdown = SafeToSpendBreakdown(
            income = Money(income, currency),
            spent = Money(-spent, currency),
            plannedBudgets = Money(0, currency),
            upcomingBills = Money(0, currency),
            clampAdjustment = Money(adjustment, currency),
            total = Money(total, currency),
        )
        return SafeToSpend(
            total = Money(total, currency),
            daysRemaining = days,
            dailyAmount = Money(daily, currency),
            breakdown = breakdown,
        )
    }
}