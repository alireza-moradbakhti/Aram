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
class DashboardTrendCardTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `card shows the six month labels and the in-out legend`() {
        setDashboard(
            TrendUiState(
                buckets = listOf(
                    MonthBucket("Jun", incomeMinor = 0L, expenseMinor = 0L),
                    MonthBucket("Jul", incomeMinor = 0L, expenseMinor = 0L),
                    MonthBucket("Aug", incomeMinor = 2_000_000L, expenseMinor = 1_000_000L),
                    MonthBucket("Sep", incomeMinor = 0L, expenseMinor = 0L),
                    MonthBucket("Oct", incomeMinor = 0L, expenseMinor = 900_000L),
                    MonthBucket("Nov", incomeMinor = 5_000_000L, expenseMinor = 300_000L),
                ),
            ),
        )

        composeRule.onNodeWithText("Six months").assertExists()
        composeRule.onNodeWithText("In").assertExists()
        composeRule.onNodeWithText("Out").assertExists()
        listOf("Jun", "Jul", "Aug", "Sep", "Oct", "Nov").forEach { label ->
            composeRule.onNodeWithText(label).assertExists()
        }
    }

    @Test
    fun `empty history replaces the labels with the empty copy`() {
        setDashboard(TrendUiState())

        composeRule.onNodeWithText("Six months").assertExists()
        composeRule.onNodeWithText("No history yet — your last six months will show here.")
            .assertExists()
        composeRule.onNodeWithText("Jun").assertDoesNotExist()
    }

    private fun setDashboard(trend: TrendUiState) {
        composeRule.setContent {
            AramTheme(darkTheme = true) {
                DashboardScreen(displayName = "Aram", trend = trend)
            }
        }
    }
}