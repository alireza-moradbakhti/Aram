package com.aram.feature.transactions.presentation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
import com.aram.core.designsystem.component.AramChip
import com.aram.core.designsystem.component.AramGhostButton
import com.aram.core.designsystem.component.AramGlassButton
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
        onRetryQueued = viewModel::onRetryQueued,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onApplyFilters = viewModel::onApplyFilters,
        onClearFilters = viewModel::onClearFilters,
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
    onRetryQueued: () -> Unit = {},
    onSearchQueryChange: (String) -> Unit = {},
    onApplyFilters: (TransactionFiltersUi) -> Unit = {},
    onClearFilters: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val month = state.month
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var searchText by rememberSaveable { mutableStateOf("") }
    var filterSheetOpen by rememberSaveable { mutableStateOf(false) }
    val clearAll = {
        searchText = ""
        searchOpen = false
        onClearFilters()
    }

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
                if (searchOpen) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SearchField(
                            value = searchText,
                            onValueChange = {
                                searchText = it
                                onSearchQueryChange(it)
                            },
                            onClose = {
                                searchText = ""
                                onSearchQueryChange("")
                                searchOpen = false
                            },
                            modifier = Modifier.weight(1f),
                        )
                        FilterAction(
                            active = state.filters.isActive,
                            onClick = { filterSheetOpen = true },
                        )
                    }
                } else {
                    AramAppBar(title = "Activity") {
                        AramIconButton(
                            icon = AramIcons.search,
                            contentDescription = "Search transactions",
                            onClick = { searchOpen = true },
                        )
                        FilterAction(
                            active = state.filters.isActive,
                            onClick = { filterSheetOpen = true },
                        )
                    }
                }
            }
        }

        if (state.isFiltering) {
            item(key = "results-header") {
                ResultsHeader(
                    count = state.entryCount,
                    canClear = state.scope != TransactionScope.ALL ||
                        state.filters.isActive ||
                        state.query.isNotBlank(),
                    onClearAll = clearAll,
                )
            }
            val chips = appliedChips(state)
            if (chips.isNotEmpty()) {
                item(key = "applied-chips") {
                    AppliedChipsRow(chips)
                }
            }
        } else {
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

        if (state.queueMessage() != null) {
            item(key = "outbox-banner") {
                OutboxBanner(
                    message = state.queueMessage() ?: "",
                    action = if (state.outbox.failed > 0) "Retry" else "Sync now",
                    onAction = onRetryQueued,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }

        if (!state.isFiltering) {
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

            state.isFiltering && state.noMatches -> item(key = "no-matches") {
                NoMatchesCard(
                    query = state.query,
                    onClear = clearAll,
                )
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

    if (filterSheetOpen) {
        FilterSheet(
            categories = state.categories,
            initial = state.filters,
            onDismiss = { filterSheetOpen = false },
            onApply = { applied ->
                filterSheetOpen = false
                onApplyFilters(applied)
            },
        )
    }
}

private val SYNC_BANNER_STATUSES = setOf(
    SyncStatus.OFFLINE,
    SyncStatus.STALE,
    SyncStatus.FAILED,
)

@Composable
private fun FilterAction(active: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.aramColors
    Box {
        AramIconButton(
            icon = AramIcons.filter,
            contentDescription = "Filter transactions",
            onClick = onClick,
        )
        if (active) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 8.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(colors.primaryHi),
            )
        }
    }
}

@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.aramColors
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    Row(
        modifier = modifier
            .height(48.dp)
            .clip(CircleShape)
            .background(colors.surface1)
            .border(1.dp, colors.border, CircleShape)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = AramIcons.search,
            contentDescription = null,
            tint = colors.textFaint,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(
                    text = "Search notes",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textFaint,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
            )
        }
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .clickable {
                    keyboard?.hide()
                    onClose()
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = AramIcons.close,
                contentDescription = "Close search",
                tint = colors.textDim,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun ResultsHeader(
    count: Int,
    canClear: Boolean,
    onClearAll: () -> Unit,
) {
    val colors = MaterialTheme.aramColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = if (count == 1) "1 result" else "$count results",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "Across all months",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
                color = colors.textFaint,
            )
        }
        if (canClear) {
            TextButton(onClick = onClearAll) {
                Text(
                    text = "Clear all",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                )
            }
        }
    }
}

@Composable
private fun AppliedChipsRow(chips: List<String>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        chips.forEach { chip -> AramChip(text = chip) }
    }
}

/** The scope is already visible in the segmented control, so chips cover only category and date. */
private fun appliedChips(state: TransactionsUiState): List<String> = buildList {
    state.filters.categoryId?.let { id ->
        add(state.categories.firstOrNull { it.id == id }?.name ?: "Category")
    }
    if (state.filters.dateRange != DateRangePreset.ANY) {
        add(state.filters.dateRange.label)
    }
}

@Composable
private fun NoMatchesCard(
    query: String,
    onClear: () -> Unit,
) {
    val colors = MaterialTheme.aramColors
    val title = if (query.isNotBlank()) {
        "No matches for \u201C$query\u201D"
    } else {
        "No matching transactions"
    }
    GlassCard(
        shape = RoundedCornerShape(22.dp),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 28.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth(),
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
                    imageVector = AramIcons.search,
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
                text = "Try a shorter word, or clear the category filter to see everything.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textFaint,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            AramGlassButton(
                text = "Clear filters",
                onClick = onClear,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FilterSheet(
    categories: List<CategoryOptionUi>,
    initial: TransactionFiltersUi,
    onDismiss: () -> Unit,
    onApply: (TransactionFiltersUi) -> Unit,
) {
    val colors = MaterialTheme.aramColors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var categoryId by remember { mutableStateOf(initial.categoryId) }
    var dateRange by remember { mutableStateOf(initial.dateRange) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.borderStrong),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Filter",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                AramIconButton(
                    icon = AramIcons.close,
                    contentDescription = "Close",
                    onClick = onDismiss,
                )
            }

            Spacer(Modifier.height(14.dp))
            SheetSectionLabel("Category")
            Spacer(Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterCategoryChip(
                    label = "All categories",
                    selected = categoryId == null,
                    onClick = { categoryId = null },
                )
                categories.forEach { category ->
                    FilterCategoryChip(
                        label = category.name,
                        icon = categoryIcon(category.iconKey),
                        selected = categoryId == category.id,
                        onClick = {
                            categoryId = if (categoryId == category.id) null else category.id
                        },
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            SheetSectionLabel("Date range")
            Spacer(Modifier.height(10.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.surface1)
                    .border(1.dp, colors.border, RoundedCornerShape(16.dp)),
            ) {
                DateRangePreset.entries.forEachIndexed { index, preset ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { dateRange = preset }
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = preset.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (dateRange == preset) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                colors.textDim
                            },
                            modifier = Modifier.weight(1f),
                        )
                        if (dateRange == preset) {
                            Icon(
                                imageVector = AramIcons.check,
                                contentDescription = null,
                                tint = colors.primaryHi,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                    if (index != DateRangePreset.entries.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            color = colors.border,
                            thickness = 1.dp,
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AramGhostButton(
                    text = "Reset",
                    onClick = {
                        categoryId = null
                        dateRange = DateRangePreset.ANY
                    },
                    modifier = Modifier.weight(1f),
                )
                AramPrimaryButton(
                    text = "Apply",
                    onClick = {
                        onApply(TransactionFiltersUi(categoryId = categoryId, dateRange = dateRange))
                    },
                    modifier = Modifier.weight(1.4f),
                )
            }
        }
    }
}

@Composable
private fun SheetSectionLabel(text: String) {
    val colors = MaterialTheme.aramColors
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.5.sp),
        color = colors.textFaint,
    )
}

@Composable
private fun FilterCategoryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector? = null,
) {
    val colors = MaterialTheme.aramColors
    Row(
        modifier = Modifier
            .height(38.dp)
            .clip(CircleShape)
            .background(if (selected) colors.successSoft else colors.surface1)
            .border(1.dp, if (selected) Color(0x470E9F6E) else colors.border, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) colors.primaryHi else colors.textFaint,
                modifier = Modifier.size(15.dp),
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.sp),
            color = if (selected) colors.primaryHi else colors.textDim,
        )
    }
}

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

private fun TransactionsUiState.queueMessage(): String? = when {
    outbox.failed == 1 -> "1 change couldn't sync"
    outbox.failed > 1 -> "${outbox.failed} changes couldn't sync"
    outbox.pending == 1 -> "1 change waiting to sync"
    outbox.pending > 1 -> "${outbox.pending} changes waiting to sync"
    else -> null
}

@Composable
private fun OutboxBanner(
    message: String,
    action: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.aramColors
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
                text = "$message · saved on this device",
                style = MaterialTheme.typography.labelMedium,
                color = colors.textDim,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onAction) {
                Text(
                    text = action,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                )
            }
        }
    }
}

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
            Column(horizontalAlignment = Alignment.End) {
                MoneyLabel(
                    amount = transaction.amountMinor,
                    incoming = transaction.incoming,
                    fractionDigits = transaction.fractionDigits,
                )
                if (transaction.pendingSync) {
                    Text(
                        text = "Queued",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
                        color = colors.textFaint,
                    )
                }
            }
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

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun TransactionsScreenNoMatchesPreview() {
    AramTheme(darkTheme = true) {
        TransactionsScreen(
            state = previewState().copy(
                groups = emptyList(),
                entryCount = 0,
                filters = TransactionFiltersUi(
                    categoryId = "food",
                    dateRange = DateRangePreset.LAST_30_DAYS,
                ),
                query = "coffee",
                isFiltering = true,
                noMatches = true,
            ),
            onPreviousMonth = {},
            onNextMonth = {},
            onScopeChange = {},
            onAddTransaction = {},
            onRefresh = {},
        )
    }
}
