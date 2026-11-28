package com.aram.feature.dashboard.presentation

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.aram.core.designsystem.AramTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w390dp-h2400dp-xhdpi")
class DashboardDonutCardTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `empty month keeps the card copy instead of legend rows`() {
        setDashboard(DonutUiState(monthLabel = "November"))

        composeRule.onNodeWithText("Where it went").assertExists()
        composeRule.onNodeWithText("No expenses yet this month").assertExists()
        composeRule.onNodeWithText("0").assertExists()
    }

    @Test
    fun `legend lists the top slices with percentages and amounts`() {
        val slices = buildDonutSlices(
            listOf(
                CategorySpend("housing", "Housing", 0xFF3B82F6.toInt(), 12_000_000),
                CategorySpend("groceries", "Groceries", 0xFF10B981.toInt(), 2_430_000),
            ),
        )
        setDashboard(
            DonutUiState(
                monthLabel = "November",
                totalMinor = 14_430_000,
                slices = slices,
            ),
        )

        composeRule.onNodeWithText("Housing").assertExists()
        composeRule.onNodeWithText("12,000,000").assertExists()
        composeRule.onNodeWithText("83%").assertExists()
        composeRule.onNodeWithText("2,430,000").assertExists()
        composeRule.onNodeWithText("17%").assertExists()
    }

    private fun setDashboard(donut: DonutUiState) {
        composeRule.setContent {
            AramTheme(darkTheme = true) {
                DashboardScreen(displayName = "Aram", donut = donut)
            }
        }
    }
}