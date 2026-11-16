package com.aram.feature.budgets.presentation

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun BudgetsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val colors = MaterialTheme.aramColors
    var monthOffset by remember { mutableIntStateOf(0) }
    val month = YearMonth.of(2026, 10).plusMonths(monthOffset.toLong())
    val monthTitle = remember(month) {
        month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US))
    }
    val resetDay = month.plusMonths(1).month.getDisplayName(TextStyle.SHORT, Locale.US)

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
                onClick = { toast(context, "Budget settings (demo)") },
            )
        }

        AramMonthNav(
            title = monthTitle,
            subtitle = "Resets 1 $resetDay · 20 days left",
            onPrevious = { monthOffset-- },
            onNext = { monthOffset++ },
        )

        GlassCard(
            modifier = Modifier.padding(top = 4.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
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
                AramChip(text = "On track", tone = AramChipTone.Positive)
            }
            Row(
                modifier = Modifier.padding(top = 12.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = formatAmount(12_000_000L),
                    style = AramTextStyles.heroAmountCompact,
                )
                Text(
                    text = "IRR",
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
                MoneyLine(text = "${formatAmount(8_160_000L)} spent")
                MoneyLine(text = "${formatAmount(3_840_000L)} left")
            }
            AramBar(
                progress = 0.68f,
                modifier = Modifier.padding(top = 10.dp),
                height = 10,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                MoneyLine(text = "≈ 137,000 / day", faint = true)
                MoneyLine(text = "32% remaining", faint = true)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "Categories", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "4 active",
                style = MaterialTheme.typography.labelSmall,
                color = colors.textFaint,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            budgetItems.forEach { item ->
                BudgetRow(item = item) { toast(context, "Edit ${item.name} budget") }
            }
        }

        AramPrimaryButton(
            text = "New budget",
            icon = AramIcons.add,
            onClick = { toast(context, "New budget sheet (demo)") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
        )
        Text(
            text = "Unused budget rolls into Savings on 1 $resetDay.",
            style = MaterialTheme.typography.labelSmall,
            color = colors.textFaint,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
private fun BudgetRow(item: BudgetItemUi, onClick: () -> Unit) {
    val colors = MaterialTheme.aramColors
    val (iconTint, iconBackground, iconBorder) = when (item.tone) {
        BudgetTone.Ok -> Triple(colors.primaryHi, colors.successSoft, Color(0x420E9F6E))
        BudgetTone.Warn -> Triple(colors.secondaryHi, colors.warningSoft, Color(0x47D97706))
        BudgetTone.Danger -> Triple(colors.danger, colors.dangerSoft, Color(0x3DFFB4AB))
    }
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 12.dp),
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
                    imageVector = item.icon,
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
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "${formatAmount(item.spent)} / ${formatAmount(item.limit)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textFaint,
                    )
                }
                AramBar(
                    progress = item.progress,
                    modifier = Modifier.padding(top = 6.dp),
                    tone = item.barTone,
                    height = 6,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = item.foot,
                        style = MaterialTheme.typography.labelSmall,
                        color = when (item.tone) {
                            BudgetTone.Warn -> colors.secondaryHi
                            BudgetTone.Danger -> colors.danger
                            BudgetTone.Ok -> colors.textFaint
                        },
                    )
                    Text(
                        text = item.rightFoot,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textFaint,
                    )
                }
            }
        }
    }
}

@Composable
private fun MoneyLine(text: String, faint: Boolean = false) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = if (faint) MaterialTheme.aramColors.textFaint else MaterialTheme.aramColors.textDim,
    )
}

private enum class BudgetTone { Ok, Warn, Danger }

private data class BudgetItemUi(
    val name: String,
    val spent: Long,
    val limit: Long,
    val progress: Float,
    val icon: ImageVector,
    val tone: BudgetTone,
    val foot: String,
    val rightFoot: String,
) {
    val barTone: AramBarTone
        get() = when (tone) {
            BudgetTone.Ok -> AramBarTone.Positive
            BudgetTone.Warn -> AramBarTone.Warning
            BudgetTone.Danger -> AramBarTone.Danger
        }
}

private val budgetItems = listOf(
    BudgetItemUi(
        name = "Groceries",
        spent = 4_120_000L,
        limit = 6_000_000L,
        progress = 0.68f,
        icon = AramIcons.cart,
        tone = BudgetTone.Ok,
        foot = "68% used",
        rightFoot = "1,880,000 left",
    ),
    BudgetItemUi(
        name = "Dining out",
        spent = 1_760_000L,
        limit = 2_000_000L,
        progress = 0.88f,
        icon = AramIcons.restaurant,
        tone = BudgetTone.Warn,
        foot = "88% used · near limit",
        rightFoot = "240,000 left",
    ),
    BudgetItemUi(
        name = "Transport",
        spent = 1_050_000L,
        limit = 2_500_000L,
        progress = 0.42f,
        icon = AramIcons.car,
        tone = BudgetTone.Ok,
        foot = "42% used",
        rightFoot = "1,450,000 left",
    ),
    BudgetItemUi(
        name = "Subscriptions",
        spent = 672_000L,
        limit = 600_000L,
        progress = 1f,
        icon = AramIcons.musicNote,
        tone = BudgetTone.Danger,
        foot = "112% used · over by 72,000",
        rightFoot = "0 left",
    ),
)

private fun toast(context: android.content.Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}
