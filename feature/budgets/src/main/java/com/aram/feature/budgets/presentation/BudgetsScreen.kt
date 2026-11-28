package com.aram.feature.budgets.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aram.core.designsystem.AramTextStyles
import com.aram.core.designsystem.aramColors
import com.aram.core.designsystem.component.AramAppBar
import com.aram.core.designsystem.component.AramBar
import com.aram.core.designsystem.component.AramBarTone
import com.aram.core.designsystem.component.AramChip
import com.aram.core.designsystem.component.AramChipTone
import com.aram.core.designsystem.component.AramIconButton
import com.aram.core.designsystem.component.AramMonthNav
import com.aram.core.designsystem.component.AramPrimaryButton
import com.aram.core.designsystem.component.GlassCard
import com.aram.core.designsystem.icon.AramIcons
import com.aram.core.designsystem.text.formatAmount
import com.aram.core.designsystem.text.formatMinor
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

internal const val BUDGETS_SKELETON_TAG = "budgets-skeleton"
internal const val TOTAL_PROGRESS_TAG = "budget-progress-total"
internal fun rowProgressTag(id: String): String = "budget-progress-$id"

@Composable
fun BudgetsRoute(
    onCreateBudget: () -> Unit = {},
    onOpenBudget: (String) -> Unit = {},
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: BudgetsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    BudgetsScreen(
        state = state,
        onPreviousMonth = viewModel::onPreviousMonth,
        onNextMonth = viewModel::onNextMonth,
        onCurrentMonth = viewModel::onCurrentMonth,
        onCreateBudget = onCreateBudget,
        onOpenBudget = onOpenBudget,
        onOpenSettings = onOpenSettings,
        modifier = modifier,
    )
}

@Composable
fun BudgetsScreen(
    state: BudgetsUiState,
    modifier: Modifier = Modifier,
    onPreviousMonth: () -> Unit = {},
    onNextMonth: () -> Unit = {},
    onCurrentMonth: () -> Unit = {},
    onCreateBudget: () -> Unit = {},
    onOpenBudget: (String) -> Unit = {},
    onOpenSettings: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 24.dp),
    ) {
        AramAppBar(title = "Budgets") {
            AramIconButton(
                icon = AramIcons.tune,
                contentDescription = "Budget settings",
                onClick = onOpenSettings,
            )
        }

        AramMonthNav(
            title = monthTitle(state.month),
            subtitle = monthSubtitle(state),
            onPrevious = onPreviousMonth,
            onNext = onNextMonth,
            onCurrentMonth = onCurrentMonth.takeUnless { state.isCurrentMonth },
        )

        when {
            state.loading -> BudgetsSkeleton()
            state.isEmpty -> EmptyBudgets(
                month = state.month,
                isCurrentMonth = state.isCurrentMonth,
                onCreateBudget = onCreateBudget,
            )
            else -> BudgetsContent(state = state, onCreateBudget = onCreateBudget, onOpenBudget = onOpenBudget)
        }
    }
}

@Composable
private fun BudgetsContent(
    state: BudgetsUiState,
    onCreateBudget: () -> Unit,
    onOpenBudget: (String) -> Unit,
) {
    val fractionDigits = state.fractionDigits
    TotalBudgetCard(state = state, fractionDigits = fractionDigits)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = "Categories", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "${state.rows.size} active",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.aramColors.textFaint,
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        state.rows.forEach { row ->
            BudgetRow(item = row, onClick = { onOpenBudget(row.id) })
        }
    }

    AramPrimaryButton(
        text = "New budget",
        icon = AramIcons.add,
        onClick = onCreateBudget,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
    )
    Text(
        text = rolloverNote(state.month),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.aramColors.textFaint,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun TotalBudgetCard(state: BudgetsUiState, fractionDigits: Int) {
    val colors = MaterialTheme.aramColors
    val spent = formatMinor(state.totalSpentMinor, fractionDigits)
    val left = formatMinor(state.totalRemainingMinor, fractionDigits)
    val limit = formatMinor(state.totalLimitMinor, fractionDigits)
    val spoken = "Total budget: $limit ${state.currencyCode}. " +
        "$spent spent, $left left, ${state.totalPercentUsed}% used."
    val (chipText, chipTone) = when (state.status) {
        BudgetStatus.ON_TRACK -> "On track" to AramChipTone.Positive
        BudgetStatus.NEAR_LIMIT -> "Near limit" to AramChipTone.Warning
        BudgetStatus.OVER -> "Over budget" to AramChipTone.Danger
    }
    GlassCard(
        modifier = Modifier
            .padding(top = 4.dp)
            .semantics(mergeDescendants = true) { contentDescription = spoken },
        contentPadding = PaddingValues(20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "TOTAL BUDGET",
                fontSize = 11.sp,
                letterSpacing = 0.9.sp,
                color = colors.textFaint,
            )
            AramChip(text = chipText, tone = chipTone)
        }
        Row(
            modifier = Modifier.padding(top = 12.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = formatAmount(state.totalLimitMinor),
                style = AramTextStyles.heroAmountCompact,
            )
            Text(
                text = state.currencyCode,
                style = MaterialTheme.typography.labelSmall,
                color = colors.textFaint,
                modifier = Modifier.padding(start = 6.dp, bottom = 4.dp),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            MoneyLine(text = "$spent spent")
            MoneyLine(text = "$left left")
        }
        BudgetBar(
            fraction = state.totalFraction,
            tone = when (state.status) {
                BudgetStatus.ON_TRACK -> AramBarTone.Positive
                BudgetStatus.NEAR_LIMIT -> AramBarTone.Warning
                BudgetStatus.OVER -> AramBarTone.Danger
            },
            height = 10,
            modifier = Modifier
                .padding(top = 10.dp)
                .testTag(TOTAL_PROGRESS_TAG),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val perDay = if (state.daysLeft > 0) {
                "≈ ${formatMinor(state.perDayMinor, fractionDigits)} / day"
            } else {
                "Month ended"
            }
            MoneyLine(text = perDay, faint = true)
            MoneyLine(
                text = "${(100 - state.totalPercentUsed).coerceAtLeast(0)}% remaining",
                faint = true,
            )
        }
    }
}

@Composable
private fun BudgetRow(item: BudgetRowUi, onClick: () -> Unit) {
    val colors = MaterialTheme.aramColors
    val (iconTint, iconBackground, iconBorder) = when (item.tone) {
        BudgetRowTone.OK -> Triple(colors.primaryHi, colors.successSoft, Color(0x420E9F6E))
        BudgetRowTone.NEAR_LIMIT -> Triple(colors.secondaryHi, colors.warningSoft, Color(0x47D97706))
        BudgetRowTone.OVER -> Triple(colors.danger, colors.dangerSoft, Color(0x3DFFB4AB))
    }
    val spent = formatMinor(item.spentMinor, item.fractionDigits)
    val limit = formatMinor(item.limitMinor, item.fractionDigits)
    val left = "${formatMinor(item.remainingMinor, item.fractionDigits)} left"
    val over = " over by ${formatMinor(item.overByMinor, item.fractionDigits)}"
    val spoken = "${item.name} budget: $limit ${item.currencyCode} limit, $spent spent, " +
        "$left, ${item.percentUsed}% used" +
        if (item.tone == BudgetRowTone.OVER) "$over." else "."
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = spoken },
        shape = RoundedCornerShape(18.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(iconBackground)
                    .border(1.dp, iconBorder, RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = budgetCategoryIcon(item.iconKey),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp),
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = item.name, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "$spent / $limit",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textFaint,
                    )
                }
                BudgetBar(
                    fraction = item.fraction,
                    tone = when (item.tone) {
                        BudgetRowTone.OK -> AramBarTone.Positive
                        BudgetRowTone.NEAR_LIMIT -> AramBarTone.Warning
                        BudgetRowTone.OVER -> AramBarTone.Danger
                    },
                    height = 6,
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .testTag(rowProgressTag(item.id)),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = when (item.tone) {
                            BudgetRowTone.OVER ->
                                "${item.percentUsed}% used ·" + over
                            BudgetRowTone.NEAR_LIMIT ->
                                "${item.percentUsed}% used · near limit"
                            BudgetRowTone.OK -> "${item.percentUsed}% used"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = when (item.tone) {
                            BudgetRowTone.OVER -> colors.danger
                            BudgetRowTone.NEAR_LIMIT -> colors.secondaryHi
                            BudgetRowTone.OK -> colors.textFaint
                        },
                    )
                    Text(
                        text = left,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textFaint,
                    )
                }
            }
        }
    }
}

/** Progress semantics belong on the bar so TalkBack reads a value, not just a hue. */
@Composable
private fun BudgetBar(
    fraction: Float,
    tone: AramBarTone,
    height: Int,
    modifier: Modifier = Modifier,
) {
    AramBar(
        progress = fraction,
        tone = tone,
        height = height,
        modifier = modifier.semantics {
            progressBarRangeInfo =
                androidx.compose.ui.semantics.ProgressBarRangeInfo(fraction, 0f..1f)
        },
    )
}

@Composable
private fun EmptyBudgets(month: YearMonth, isCurrentMonth: Boolean, onCreateBudget: () -> Unit) {
    val colors = MaterialTheme.aramColors
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 28.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(colors.surface2)
                    .border(1.dp, colors.border, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = AramIcons.budgets,
                    contentDescription = null,
                    tint = colors.textDim,
                    modifier = Modifier.size(24.dp),
                )
            }
            Text(
                text = if (isCurrentMonth) "No budgets yet" else "No budgets in ${monthTitle(month)}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 14.dp),
            )
            Text(
                text = "Set one cap for groceries and Aram will watch the rest of the month for you.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textDim,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp),
            )
            Spacer(Modifier.height(18.dp))
            AramPrimaryButton(
                text = "Create budget",
                icon = AramIcons.add,
                onClick = onCreateBudget,
            )
        }
    }
}

@Composable
private fun BudgetsSkeleton() {
    val colors = MaterialTheme.aramColors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(BUDGETS_SKELETON_TAG),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        GlassCard(
            modifier = Modifier.padding(top = 4.dp),
            contentPadding = PaddingValues(20.dp),
        ) {
            SkeletonBlock(widthFraction = 0.4f, height = 10.dp, color = colors.surface2)
            SkeletonBlock(
                widthFraction = 0.55f,
                height = 30.dp,
                color = colors.surface2,
                modifier = Modifier.padding(top = 14.dp),
            )
            SkeletonBlock(
                widthFraction = 1f,
                height = 10.dp,
                color = colors.surface2,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
        repeat(3) {
            GlassCard(
                shape = RoundedCornerShape(18.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            ) {
                SkeletonBlock(widthFraction = 0.5f, height = 12.dp, color = colors.surface2)
                SkeletonBlock(
                    widthFraction = 1f,
                    height = 6.dp,
                    color = colors.surface2,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun SkeletonBlock(
    widthFraction: Float,
    height: androidx.compose.ui.unit.Dp,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth(widthFraction)
            .height(height)
            .clip(CircleShape)
            .background(color),
    )
}

@Composable
private fun MoneyLine(text: String, faint: Boolean = false) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = if (faint) MaterialTheme.aramColors.textFaint else MaterialTheme.aramColors.textDim,
    )
}

internal fun budgetCategoryIcon(iconKey: String): ImageVector = when (iconKey) {
    "wallet" -> AramIcons.wallet
    "food" -> AramIcons.cafe
    "groceries" -> AramIcons.cart
    "transport" -> AramIcons.car
    "home" -> AramIcons.home
    "utilities" -> AramIcons.receipt
    "shopping" -> AramIcons.shoppingBag
    "health" -> AramIcons.shield
    "fun" -> AramIcons.smile
    "subscriptions" -> AramIcons.musicNote
    "salary" -> AramIcons.arrowIn
    "freelance" -> AramIcons.trending
    else -> AramIcons.receipt
}

internal fun monthTitle(month: YearMonth): String =
    month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US))

internal fun monthSubtitle(state: BudgetsUiState): String {
    val next = state.month.plusMonths(1).month.getDisplayName(TextStyle.SHORT, Locale.US)
    return when {
        state.daysLeft <= 0 -> "Closed 1 $next"
        state.isCurrentMonth -> "Resets 1 $next · ${daysLabel(state.daysLeft)} left"
        else -> "Starts 1 ${state.month.month.getDisplayName(TextStyle.SHORT, Locale.US)} · " +
            daysLabel(state.daysLeft)
    }
}

private fun daysLabel(days: Int): String = "$days ${if (days == 1) "day" else "days"}"

private fun rolloverNote(month: YearMonth): String {
    val next = month.plusMonths(1).month.getDisplayName(TextStyle.SHORT, Locale.US)
    return "Unused budget rolls into Savings on 1 $next."
}