package com.aram.feature.dashboard.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aram.core.designsystem.AramTextStyles
import com.aram.core.designsystem.aramColors
import com.aram.core.designsystem.component.AramIconButton
import com.aram.core.designsystem.icon.AramIcons
import com.aram.core.designsystem.text.formatMinor
import com.aram.core.domain.safetospend.SafeToSpend
import com.aram.core.money.CurrencyCode
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/** One sentence the sheet suggests, chosen from the result's shape. */
internal fun explainerAdvice(result: SafeToSpend?, month: YearMonth?): String {
    val monthName = month?.month?.getDisplayName(TextStyle.FULL, Locale.getDefault()) ?: "this month"
    if (result == null) {
        return "Add income and spending to see how far your money can stretch this month."
    }
    val breakdown = result.breakdown
    val currency = result.total.currency
    return when {
        breakdown.clampAdjustment.amountMinor > 0 ->
            "Planned spending is higher than income, so safe-to-spend is pinned at zero. " +
                "Trim a budget or add income to free up room."

        breakdown.income.amountMinor <= 0 ->
            "No income is recorded for $monthName yet. Add it to see how much is safe to spend."

        else ->
            "Keep daily spending at or under " +
                "${formatMinor(result.dailyAmount.amountMinor, currency.fractionDigits)} " +
                "${currency.isoCode} to finish $monthName on track."
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SafeToSpendExplainerSheet(
    result: SafeToSpend?,
    month: YearMonth?,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.aramColors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        dragHandle = {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .size(width = 38.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(colors.borderStrong),
            )
        },
    ) {
        SafeToSpendExplainerContent(result = result, month = month, onDismiss = onDismiss)
    }
}

@Composable
internal fun SafeToSpendExplainerContent(
    result: SafeToSpend?,
    month: YearMonth?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.aramColors
    val currency = result?.total?.currency ?: CurrencyCode.IRR
    val digits = currency.fractionDigits
    val breakdown = result?.breakdown
    val monthName = month?.month?.getDisplayName(TextStyle.FULL, Locale.getDefault())

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "How safe-to-spend works",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() },
            )
            AramIconButton(
                icon = AramIcons.close,
                contentDescription = "Close",
                onClick = onDismiss,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Your income this month, minus what you've spent and what's still " +
                "committed, spread over the days left.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textDim,
        )
        Spacer(Modifier.height(18.dp))

        ExplainerLine(
            icon = AramIcons.arrowIn,
            label = "Income this month",
            minor = breakdown?.income?.amountMinor ?: 0L,
            digits = digits,
            isoCode = currency.isoCode,
        )
        ExplainerLine(
            icon = AramIcons.cart,
            label = "Spent so far",
            minor = breakdown?.spent?.amountMinor ?: 0L,
            digits = digits,
            isoCode = currency.isoCode,
        )
        ExplainerLine(
            icon = AramIcons.budgets,
            label = "Planned budgets",
            minor = breakdown?.plannedBudgets?.amountMinor ?: 0L,
            digits = digits,
            isoCode = currency.isoCode,
        )
        ExplainerLine(
            icon = AramIcons.receipt,
            label = "Upcoming bills",
            minor = breakdown?.upcomingBills?.amountMinor ?: 0L,
            digits = digits,
            isoCode = currency.isoCode,
        )
        val adjustment = breakdown?.clampAdjustment?.amountMinor ?: 0L
        if (adjustment != 0L) {
            ExplainerLine(
                icon = AramIcons.alert,
                label = "Overspend adjustment",
                minor = adjustment,
                digits = digits,
                isoCode = currency.isoCode,
            )
        }
        HorizontalDivider(
            color = colors.border,
            modifier = Modifier.padding(vertical = 10.dp),
        )
        ExplainerLine(
            icon = AramIcons.wallet,
            label = "Safe to spend",
            minor = breakdown?.total?.amountMinor ?: 0L,
            digits = digits,
            isoCode = currency.isoCode,
            emphasized = true,
        )
        HorizontalDivider(
            color = colors.border,
            modifier = Modifier.padding(vertical = 10.dp),
        )
        val days = result?.daysRemaining ?: 0
        val daysText = "$days ${if (days == 1) "day" else "days"}"
        ExplainerLine(
            icon = AramIcons.clock,
            label = "Days left",
            text = daysText,
            spoken = if (monthName != null) "$daysText left in $monthName" else "$daysText left",
        )
        ExplainerLine(
            icon = AramIcons.trending,
            label = "Daily amount",
            text = if (result != null) {
                "\u2248 ${formatMinor(result.dailyAmount.amountMinor, digits)} ${currency.isoCode}"
            } else {
                "\u2014"
            },
            spoken = if (result != null) {
                "${formatMinor(result.dailyAmount.amountMinor, digits)} ${currency.isoCode} a day"
            } else {
                null
            },
        )
        Spacer(Modifier.height(16.dp))
        AdviceCard(text = explainerAdvice(result, month))
    }
}

@Composable
private fun ExplainerLine(
    icon: ImageVector,
    label: String,
    minor: Long,
    digits: Int,
    isoCode: String,
    emphasized: Boolean = false,
) {
    val value = if (minor < 0) {
        "\u2212${formatMinor(-minor, digits)}"
    } else {
        formatMinor(minor, digits)
    }
    val spoken = if (minor < 0) {
        "minus ${formatMinor(-minor, digits)} $isoCode"
    } else {
        "${formatMinor(minor, digits)} $isoCode"
    }
    ExplainerRow(
        icon = icon,
        label = label,
        value = value,
        spokenDescription = "$label: $spoken",
        emphasized = emphasized,
    )
}

@Composable
private fun ExplainerLine(
    icon: ImageVector,
    label: String,
    text: String,
    spoken: String?,
) {
    ExplainerRow(
        icon = icon,
        label = label,
        value = text,
        spokenDescription = spoken?.let { "$label: $it" },
        emphasized = false,
    )
}

@Composable
private fun ExplainerRow(
    icon: ImageVector,
    label: String,
    value: String,
    spokenDescription: String?,
    emphasized: Boolean,
) {
    val colors = MaterialTheme.aramColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .padding(vertical = 9.dp)
            .semantics(mergeDescendants = true) {
                if (spokenDescription != null) contentDescription = spokenDescription
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (emphasized) colors.primaryHi else colors.textFaint,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (emphasized) MaterialTheme.colorScheme.onSurface else colors.textDim,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            style = if (emphasized) AramTextStyles.statValue else AramTextStyles.money,
            color = if (emphasized) MaterialTheme.colorScheme.onSurface else colors.textDim,
            maxLines = 1,
        )
    }
}

@Composable
private fun AdviceCard(text: String) {
    val colors = MaterialTheme.aramColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface1)
            .border(1.dp, colors.border, RoundedCornerShape(16.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = AramIcons.check,
            contentDescription = null,
            tint = colors.primaryHi,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(16.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}