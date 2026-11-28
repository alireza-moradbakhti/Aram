package com.aram.feature.budgets.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertRangeInfoEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.aram.core.designsystem.AramTheme
import java.time.YearMonth
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w390dp-h1000dp-xhdpi")
class BudgetsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `content shows repository totals and category rows`() {
        composeRule.setContent {
            Host(state = contentState())
        }

        composeRule.onNodeWithText("12,000,000", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("8,160,000 spent", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("3,840,000 left", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("Groceries", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("Dining out", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("3 active", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("On track", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithContentDescription("Total budget: 12,000,000 IRR", substring = true)
            .assertExists()
        composeRule.onNodeWithContentDescription("Groceries budget: 6,000,000 IRR limit", substring = true)
            .assertExists()
    }

    @Test
    fun `over-budget category is distinct but still a normal row`() {
        composeRule.setContent {
            Host(state = contentState())
        }

        composeRule.onNodeWithText("112% used · over by 72,000", useUnmergedTree = true)
            .assertExists()
        composeRule.onNodeWithText("0 left", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithContentDescription(
            "Subscriptions budget: 600,000 IRR limit, 672,000 spent, 0 left, 112% used over by 72,000.",
        ).assertExists()
    }

    @Test
    fun `progress bars expose a value to accessibility services`() {
        composeRule.setContent {
            Host(state = contentState())
        }

        composeRule.onNodeWithTag(TOTAL_PROGRESS_TAG, useUnmergedTree = true)
            .assertRangeInfoEquals(ProgressBarRangeInfo(0.68f, 0f..1f))
        composeRule.onNodeWithTag(rowProgressTag("budget-subscriptions"), useUnmergedTree = true)
            .assertRangeInfoEquals(ProgressBarRangeInfo(1f, 0f..1f))
    }

    @Test
    fun `empty month offers the create CTA`() {
        var created = false
        composeRule.setContent {
            Host(state = emptyState(), onCreateBudget = { created = true })
        }

        composeRule.onNodeWithText("No budgets yet", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("Create budget").performScrollTo().performClick()
        assertTrue(created)
    }

    @Test
    fun `other empty months name the month instead of saying no budgets yet`() {
        composeRule.setContent {
            Host(state = emptyState().copy(month = YearMonth.of(2026, 9), isCurrentMonth = false))
        }

        composeRule.onNodeWithText("No budgets in September 2026", useUnmergedTree = true)
            .assertExists()
        composeRule.onAllNodesWithText("No budgets yet", useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun `month shortcut appears off the current month and returns`() {
        var returned = false
        composeRule.setContent {
            Host(state = contentState().copy(isCurrentMonth = false), onCurrentMonth = { returned = true })
        }

        composeRule.onNodeWithText("This month").performClick()
        assertTrue(returned)
    }

    @Test
    fun `month shortcut is hidden on the current month`() {
        composeRule.setContent {
            Host(state = contentState())
        }

        composeRule.onAllNodesWithText("This month").assertCountEquals(0)
    }

    @Test
    fun `previous and next arrows walk the months`() {
        var previous = 0
        var next = 0
        composeRule.setContent {
            Host(state = contentState(), onPreviousMonth = { previous++ }, onNextMonth = { next++ })
        }

        composeRule.onNodeWithContentDescription("Previous month").performClick()
        composeRule.onNodeWithContentDescription("Next month").performClick()

        assertTrue(previous == 1 && next == 1)
    }

    @Test
    fun `loading renders the skeleton instead of the hero`() {
        composeRule.setContent {
            Host(state = contentState().copy(loading = true))
        }

        composeRule.onNodeWithTag(BUDGETS_SKELETON_TAG, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("TOTAL BUDGET", useUnmergedTree = true).assertDoesNotExist()
    }

    @androidx.compose.runtime.Composable
    private fun Host(
        state: BudgetsUiState,
        onCreateBudget: () -> Unit = {},
        onPreviousMonth: () -> Unit = {},
        onNextMonth: () -> Unit = {},
        onCurrentMonth: () -> Unit = {},
    ) {
        AramTheme(darkTheme = true) {
            Box(Modifier.fillMaxSize()) {
                BudgetsScreen(
                    state = state,
                    onPreviousMonth = onPreviousMonth,
                    onNextMonth = onNextMonth,
                    onCurrentMonth = onCurrentMonth,
                    onCreateBudget = onCreateBudget,
                )
            }
        }
    }

    private fun contentState(): BudgetsUiState = BudgetsUiState(
        month = YearMonth.of(2026, 11),
        isCurrentMonth = true,
        loading = false,
        currencyCode = "IRR",
        fractionDigits = 0,
        totalLimitMinor = 12_000_000,
        totalSpentMinor = 8_160_000,
        totalRemainingMinor = 3_840_000,
        totalFraction = 0.68f,
        totalPercentUsed = 68,
        daysLeft = 20,
        perDayMinor = 137_000,
        rows = listOf(
            BudgetRowUi(
                id = "budget-groceries",
                categoryId = "category-groceries",
                name = "Groceries",
                iconKey = "groceries",
                spentMinor = 4_120_000,
                limitMinor = 6_000_000,
                remainingMinor = 1_880_000,
                fraction = 0.68f,
                percentUsed = 68,
                overByMinor = 0,
                currencyCode = "IRR",
                fractionDigits = 0,
                tone = BudgetRowTone.OK,
            ),
            BudgetRowUi(
                id = "budget-dining",
                categoryId = "category-food",
                name = "Dining out",
                iconKey = "food",
                spentMinor = 1_760_000,
                limitMinor = 2_000_000,
                remainingMinor = 240_000,
                fraction = 0.88f,
                percentUsed = 88,
                overByMinor = 0,
                currencyCode = "IRR",
                fractionDigits = 0,
                tone = BudgetRowTone.NEAR_LIMIT,
            ),
            BudgetRowUi(
                id = "budget-subscriptions",
                categoryId = "category-subscriptions",
                name = "Subscriptions",
                iconKey = "subscriptions",
                spentMinor = 672_000,
                limitMinor = 600_000,
                remainingMinor = 0,
                fraction = 1f,
                percentUsed = 112,
                overByMinor = 72_000,
                currencyCode = "IRR",
                fractionDigits = 0,
                tone = BudgetRowTone.OVER,
            ),
        ),
    )

    private fun emptyState(): BudgetsUiState = BudgetsUiState(
        month = YearMonth.of(2026, 11),
        isCurrentMonth = true,
        loading = false,
    )
}