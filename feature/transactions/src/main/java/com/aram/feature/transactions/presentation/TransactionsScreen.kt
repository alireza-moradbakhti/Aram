package com.aram.feature.transactions.presentation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aram.core.designsystem.AramTextStyles
import com.aram.core.designsystem.AramTheme
import com.aram.core.designsystem.aramColors
import com.aram.core.designsystem.component.AramAppBar
import com.aram.core.designsystem.component.AramIconButton
import com.aram.core.designsystem.component.AramMonthNav
import com.aram.core.designsystem.component.AramPrimaryButton
import com.aram.core.designsystem.component.AramSegmented
import com.aram.core.designsystem.component.GlassCard
import com.aram.core.designsystem.component.MoneyLabel
import com.aram.core.designsystem.icon.AramIcons
import com.aram.core.designsystem.text.formatMinor
import com.aram.core.designsystem.text.formatSignedMinor
import com.aram.core.model.SyncState
import com.aram.core.model.SyncStatus
import com.aram.core.money.CurrencyCode
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs

@Composable
fun TransactionsRoute(
    onAddTransaction: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TransactionsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TransactionsScreen(
        state = state,
        onPreviousMonth = viewModel::onPreviousMonth,
        onNextMonth = viewModel::onNextMonth,
        onScopeChange = viewModel::onScopeChange,
        onAddTransaction = onAddTransaction,
        onRefresh = viewModel::onRefresh,
        modifier = modifier,
    )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    state: TransactionsUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onScopeChange: (TransactionScope) -> Unit,
    onAddTransaction: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val month = state.month
    PullToRefreshBox(
        isRefreshing = state.sync.status == SyncStatus.REFRESHING,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize(),
    ) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 88.dp),
    ) {
        item(key = "app-bar") {
            Box(Modifier.padding(horizontal = 16.dp)) {
                AramAppBar(title = "Activity") {
                    AramIconButton(
                        icon = AramIcons.search,
                        contentDescription = "Search transactions",
                        onClick = {},
                    )
                    AramIconButton(
                        icon = AramIcons.filter,
                        contentDescription = "Filter transactions",
                        onClick = {},
                    )
                }
            }
        }

        stickyHeader(key = "month") {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp),
            ) {
                AramMonthNav(
                    title = "${month.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} ${month.year}",
                    subtitle = "${month.lengthOfMonth()} days · ${state.entryCount} entries",
                    onPrevious = onPreviousMonth,
                    onNext = onNextMonth,
                )
            }
        }

        item(key = "scope") {
            Box(Modifier.padding(horizontal = 16.dp)) {
                AramSegmented(
                    options = listOf("All", "Expenses", "Income"),
                    selectedIndex = state.scope.ordinal,
                    onSelect = { index -> onScopeChange(TransactionScope.entries[index]) },
                )
            }
        }

        if (state.sync.status in SYNC_BANNER_STATUSES) {
            item(key = "sync-banner") {
                SyncBanner(
                    state = state.sync,
                    onRetry = onRefresh,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }

        item(key = "stats") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ActivityStat(
                    label = "Spent",
                    value = formatMinor(state.spentMinor, state.currency.fractionDigits),
                    delta = spentDelta(state),
                    positive = false,
                    modifier = Modifier.weight(1f),
                )
                ActivityStat(
                    label = "Income",
                    value = formatMinor(state.incomeMinor, state.currency.fractionDigits),
                    delta = incomeDelta(state),
                    positive = true,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        when {
            state.loading -> item(key = "loading") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            state.isEmpty -> item(key = "empty") {
                EmptyLedger(
                    state = state,
                    onAddTransaction = onAddTransaction,
                )
            }

            else -> items(state.groups, key = { group -> group.date.toEpochDay() }) { group ->
                DayGroup(
                    group = group,
                    today = state.today,
                    fractionDigits = state.currency.fractionDigits,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
    }
    }
}

private val SYNC_BANNER_STATUSES = setOf(
    SyncStatus.OFFLINE,
    SyncStatus.STALE,
    SyncStatus.FAILED,
)

@Composable
private fun SyncBanner(
    state: SyncState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.aramColors
    val message = when (state.status) {
        SyncStatus.OFFLINE -> "You're offline · showing saved transactions"
        SyncStatus.FAILED -> "Couldn't refresh · showing saved transactions"
        else -> {
            val time = state.lastSyncedAtMillis?.let {
                SYNC_TIME_FORMAT.format(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()))
            }
            if (time == null) "Couldn't refresh · showing saved transactions"
            else "Couldn't refresh · synced $time"
        }
    }
    GlassCard(
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.labelMedium,
                color = colors.textDim,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onRetry) {
                Text(
                    text = "Retry",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                )
            }
        }
    }
}

private val SYNC_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)

@Composable
private fun DayGroup(
    group: DayGroupUi,
    today: LocalDate,
    fractionDigits: Int,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.aramColors
    Column(modifier.padding(top = 16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = dayLabel(group.date, today),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.9.sp,
                ),
                color = colors.textFaint,
            )
            Text(
                text = formatSignedMinor(group.totalMinor, fractionDigits),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.sp,
                ),
                color = colors.textDim,
            )
        }
        GlassCard(
            shape = RoundedCornerShape(22.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        ) {
            group.transactions.forEachIndexed { index, transaction ->
                TransactionRow(transaction, showDivider = index != group.transactions.lastIndex)
            }
        }
    }
}

@Composable
private fun TransactionRow(
    transaction: TransactionRowUi,
    showDivider: Boolean,
) {
    val colors = MaterialTheme.aramColors
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.surface2)
                    .border(1.dp, colors.border, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = categoryIcon(transaction.categoryIconKey),
                    contentDescription = null,
                    tint = colors.textDim,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = transaction.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = transaction.detail,
                    style = MaterialTheme.typography.bodySmall.copy(letterSpacing = 0.sp),
                    color = colors.textFaint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            MoneyLabel(
                amount = transaction.amountMinor,
                incoming = transaction.incoming,
                fractionDigits = transaction.fractionDigits,
            )
        }
        if (showDivider) {
            HorizontalDivider(color = colors.border, thickness = 1.dp)
        }
    }
}

@Composable
private fun EmptyLedger(
    state: TransactionsUiState,
    onAddTransaction: () -> Unit,
) {
    val colors = MaterialTheme.aramColors
    val monthName = state.month.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
    val (title, message) = when (state.scope) {
        TransactionScope.ALL -> "Nothing in $monthName yet" to
            "Add your first expense or income and it will show up here instantly."
        TransactionScope.EXPENSES -> "No expenses in $monthName" to
            "Spending you add this month will show up here."
        TransactionScope.INCOME -> "No income in $monthName" to
            "Income you add this month will show up here."
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(colors.surface2)
                .border(1.dp, colors.border, RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = AramIcons.receipt,
                contentDescription = null,
                tint = colors.textDim,
                modifier = Modifier.size(22.dp),
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textFaint,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        AramPrimaryButton(
            text = "Add transaction",
            onClick = onAddTransaction,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ActivityStat(
    label: String,
    value: String,
    delta: String,
    positive: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.aramColors
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surface1)
            .border(1.dp, colors.border, RoundedCornerShape(18.dp))
            .padding(16.dp),
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 0.5.sp,
            color = colors.textFaint,
        )
        Spacer(Modifier.height(6.dp))
        Text(text = value, style = AramTextStyles.statValue)
        Spacer(Modifier.height(4.dp))
        Text(
            text = delta,
            style = MaterialTheme.typography.labelSmall,
            color = if (positive) colors.primaryHi else colors.secondaryHi,
        )
    }
}

private fun spentDelta(state: TransactionsUiState): String {
    val previous = state.previousSpentMinor ?: return "No spend last month"
    val previousLabel = state.month
        .minusMonths(1)
        .month
        .getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
    if (previous <= 0) return "No spend last month"
    val change = (previous - state.spentMinor) * 100.0 / previous
    return when {
        abs(change) < 0.05 -> "On par with $previousLabel"
        change > 0 -> "${oneDecimal(change)}% under $previousLabel"
        else -> "${oneDecimal(-change)}% over $previousLabel"
    }
}

private fun incomeDelta(state: TransactionsUiState): String = when (state.incomeCount) {
    0 -> "No income yet"
    1 -> "1 entry"
    else -> "${state.incomeCount} entries"
}

private fun oneDecimal(value: Double): String = String.format(Locale.US, "%.1f", value)

private fun dayLabel(date: LocalDate, today: LocalDate): String {
    val formatted = "${date.dayOfMonth} " +
        date.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).uppercase(Locale.ENGLISH)
    return when (date) {
        today -> "TODAY · $formatted"
        today.minusDays(1) -> "YESTERDAY · $formatted"
        else -> formatted
    }
}

private fun previewState(scope: TransactionScope = TransactionScope.ALL): TransactionsUiState {
    val today = LocalDate.of(2026, 10, 12)
    fun row(id: String, title: String, detail: String, iconKey: String?, amount: Long, incoming: Boolean) =
        TransactionRowUi(id, title, detail, iconKey, amount, 0, incoming)
    val groups = listOf(
        DayGroupUi(
            date = today,
            totalMinor = -420_000,
            transactions = listOf(
                row("1", "Café Nima", "Food · 09:12 · Cash", "food", -180_000, false),
                row("2", "Snapp ride", "Transport · 08:40 · Card", "transport", -240_000, false),
                row("3", "Salary", "Income · 08:00 · Melli", "salary", 85_000_000, true),
            ),
        ),
        DayGroupUi(
            date = today.minusDays(1),
            totalMinor = -1_490_000,
            transactions = listOf(
                row("4", "Grocery run", "Food · 18:20 · Card", "groceries", -1_240_000, false),
                row("5", "Spotify", "Subscriptions · 12:00 · Auto-pay", "subscriptions", -250_000, false),
            ),
        ),
    )
    return TransactionsUiState(
        month = YearMonth.of(2026, 10),
        today = today,
        scope = scope,
        loading = false,
        currency = CurrencyCode.IRR,
        groups = groups,
        entryCount = 5,
        spentMinor = 1_910_000,
        incomeMinor = 85_000_000,
        incomeCount = 1,
        previousSpentMinor = 2_040_000,
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun TransactionsScreenDarkPreview() {
    AramTheme(darkTheme = true) {
        TransactionsScreen(
            state = previewState(),
            onPreviousMonth = {},
            onNextMonth = {},
            onScopeChange = {},
            onAddTransaction = {},
            onRefresh = {},
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun TransactionsScreenLightPreview() {
    AramTheme(darkTheme = false) {
        TransactionsScreen(
            state = previewState(),
            onPreviousMonth = {},
            onNextMonth = {},
            onScopeChange = {},
            onAddTransaction = {},
            onRefresh = {},
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun TransactionsScreenEmptyPreview() {
    AramTheme(darkTheme = true) {
        TransactionsScreen(
            state = previewState().copy(
                groups = emptyList(),
                entryCount = 0,
                spentMinor = 0,
                incomeMinor = 0,
                incomeCount = 0,
                previousSpentMinor = null,
            ),
            onPreviousMonth = {},
            onNextMonth = {},
            onScopeChange = {},
            onAddTransaction = {},
            onRefresh = {},
        )
    }
}
