package com.aram.feature.transactions.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aram.core.designsystem.AramTextStyles
import com.aram.core.designsystem.AramTheme
import com.aram.core.designsystem.aramColors
import com.aram.core.designsystem.component.AramAppBar
import com.aram.core.designsystem.component.AramFab
import com.aram.core.designsystem.component.AramGlassButton
import com.aram.core.designsystem.component.AramIconButton
import com.aram.core.designsystem.component.AramMonthNav
import com.aram.core.designsystem.component.AramPrimaryButton
import com.aram.core.designsystem.component.AramSegmented
import com.aram.core.designsystem.component.GlassCard
import com.aram.core.designsystem.component.MoneyLabel
import com.aram.core.designsystem.icon.AramIcons
import com.aram.core.designsystem.text.formatAmount
import com.aram.core.designsystem.text.formatSignedAmount
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private data class DemoTransaction(
    val id: Long,
    val title: String,
    val category: String,
    val time: String,
    val method: String,
    val amount: Long,
    val day: String,
    val icon: ImageVector,
)

private val seedTransactions = listOf(
    DemoTransaction(1, "Café Nima", "Food", "09:12", "Cash", -180_000, "Today · 12 Oct", AramIcons.cafe),
    DemoTransaction(2, "Snapp ride", "Transport", "08:40", "Card", -240_000, "Today · 12 Oct", AramIcons.car),
    DemoTransaction(3, "Salary", "Income", "08:00", "Melli", 85_000_000, "Today · 12 Oct", AramIcons.arrowIn),
    DemoTransaction(4, "Grocery run", "Food", "18:20", "Card", -1_240_000, "Yesterday · 11 Oct", AramIcons.shoppingBag),
    DemoTransaction(5, "Spotify", "Subscriptions", "12:00", "Auto-pay", -250_000, "Yesterday · 11 Oct", AramIcons.musicNote),
    DemoTransaction(6, "Rent", "Housing", "10:00", "Transfer", -8_000_000, "10 Oct", AramIcons.home),
)

private val earlierTransactions = listOf(
    DemoTransaction(7, "Pharmacy", "Health", "17:05", "Card", -320_000, "09 Oct", AramIcons.shoppingBag),
    DemoTransaction(8, "Bonus", "Income", "09:00", "Melli", 5_000_000, "09 Oct", AramIcons.receipt),
)

@Composable
fun TransactionsRoute(
    modifier: Modifier = Modifier,
    requestAdd: Boolean = false,
    onAddRequestConsumed: () -> Unit = {},
) {
    TransactionsScreen(
        modifier = modifier,
        requestAdd = requestAdd,
        onAddRequestConsumed = onAddRequestConsumed,
    )
}

@Composable
fun TransactionsScreen(
    modifier: Modifier = Modifier,
    requestAdd: Boolean = false,
    onAddRequestConsumed: () -> Unit = {},
) {
    var scopeIndex by rememberSaveable { mutableIntStateOf(0) }
    var monthOffset by rememberSaveable { mutableIntStateOf(0) }
    var showEarlier by rememberSaveable { mutableStateOf(false) }
    var showSheet by rememberSaveable { mutableStateOf(false) }
    val added = remember { mutableStateListOf<DemoTransaction>() }

    LaunchedEffect(requestAdd) {
        if (requestAdd) {
            showSheet = true
            onAddRequestConsumed()
        }
    }

    val allTransactions = buildList {
        addAll(added)
        addAll(seedTransactions)
        if (showEarlier) addAll(earlierTransactions)
    }
    val visible = when (scopeIndex) {
        1 -> allTransactions.filter { it.amount < 0 }
        2 -> allTransactions.filter { it.amount > 0 }
        else -> allTransactions
    }
    val groups = LinkedHashMap<String, MutableList<DemoTransaction>>()
    visible.forEach { transaction ->
        groups.getOrPut(transaction.day) { mutableListOf() }.add(transaction)
    }

    val spent = allTransactions.filter { it.amount < 0 }.sumOf { -it.amount }
    val income = allTransactions.filter { it.amount > 0 }.sumOf { it.amount }
    val month = YearMonth.of(2026, 10).plusMonths(monthOffset.toLong())

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 88.dp),
        ) {
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

            AramMonthNav(
                title = "${month.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} ${month.year}",
                subtitle = "${month.lengthOfMonth()} days · ${allTransactions.size} entries",
                onPrevious = { monthOffset -= 1 },
                onNext = { monthOffset += 1 },
            )

            AramSegmented(
                options = listOf("All", "Expenses", "Income"),
                selectedIndex = scopeIndex,
                onSelect = { scopeIndex = it },
            )

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ActivityStat(
                    label = "Spent",
                    value = formatAmount(spent),
                    delta = "6.4% under Sep",
                    positive = false,
                    modifier = Modifier.weight(1f),
                )
                ActivityStat(
                    label = "Income",
                    value = formatAmount(income),
                    delta = "On time · 1st",
                    positive = true,
                    modifier = Modifier.weight(1f),
                )
            }

            groups.forEach { (day, transactions) ->
                val total = transactions.filter { it.amount < 0 }.sumOf { it.amount }
                Column(Modifier.padding(top = 16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = day.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.9.sp,
                            ),
                            color = MaterialTheme.aramColors.textFaint,
                        )
                        Text(
                            text = formatSignedAmount(total),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.sp,
                            ),
                            color = MaterialTheme.aramColors.textDim,
                        )
                    }
                    GlassCard(
                        shape = RoundedCornerShape(22.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    ) {
                        transactions.forEachIndexed { index, transaction ->
                            TransactionRow(transaction, showDivider = index != transactions.lastIndex)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            if (!showEarlier) {
                AramGlassButton(
                    text = "Load earlier entries",
                    onClick = { showEarlier = true },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        AramFab(
            onClick = { showSheet = true },
            contentDescription = "Add expense",
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 16.dp),
        )
    }

    if (showSheet) {
        AddExpenseSheet(
            onDismiss = { showSheet = false },
            onSave = { amount, category, note ->
                val time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
                added.add(
                    0,
                    DemoTransaction(
                        id = System.currentTimeMillis(),
                        title = note.ifBlank { "$category expense" },
                        category = category,
                        time = time,
                        method = "Cash",
                        amount = -amount,
                        day = "Today · 12 Oct",
                        icon = categoryIcon(category),
                    ),
                )
                showSheet = false
            },
        )
    }
}

@Composable
private fun TransactionRow(
    transaction: DemoTransaction,
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
                    imageVector = transaction.icon,
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
                    text = "${transaction.category} · ${transaction.time} · ${transaction.method}",
                    style = MaterialTheme.typography.bodySmall.copy(letterSpacing = 0.sp),
                    color = colors.textFaint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            MoneyLabel(amount = transaction.amount, incoming = transaction.amount > 0)
        }
        if (showDivider) {
            HorizontalDivider(color = colors.border, thickness = 1.dp)
        }
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

@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
private fun AddExpenseSheet(
    onDismiss: () -> Unit,
    onSave: (amount: Long, category: String, note: String) -> Unit,
) {
    var amountDigits by rememberSaveable { mutableStateOf("180000") }
    var category by rememberSaveable { mutableStateOf("Food") }
    var note by rememberSaveable { mutableStateOf("") }
    val parsedAmount = amountDigits.toLongOrNull() ?: 0L
    val colors = MaterialTheme.aramColors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val amountInteraction = remember { MutableInteractionSource() }
    val amountFocused by amountInteraction.collectIsFocusedAsState()
    val fxPreview = remember(parsedAmount) {
        val cents = (parsedAmount * 100 + IRR_PER_USD / 2) / IRR_PER_USD
        "$${cents / 100}.${(cents % 100).toString().padStart(2, '0')}"
    }

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
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "Add expense", style = MaterialTheme.typography.titleMedium)
                AramIconButton(
                    icon = AramIcons.close,
                    contentDescription = "Close",
                    onClick = onDismiss,
                )
            }

            Spacer(Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp)
                    .padding(bottom = 8.dp),
            ) {
                Text(
                    text = "AMOUNT",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.9.sp,
                    ),
                    color = colors.textFaint,
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    BasicTextField(
                        value = if (amountDigits.isEmpty()) "" else formatAmount(parsedAmount),
                        onValueChange = { input ->
                            amountDigits = input.filter { it.isDigit() }.take(12).trimStart('0')
                        },
                        textStyle = AramTextStyles.heroAmountCompact.copy(
                            fontSize = 34.sp,
                            lineHeight = 40.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        interactionSource = amountInteraction,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 2.dp),
                    )
                    Text(
                        text = "IRR",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textFaint,
                        modifier = Modifier.padding(start = 8.dp, bottom = 10.dp),
                    )
                }
            }

            HorizontalDivider(
                color = if (amountFocused) MaterialTheme.colorScheme.primary else colors.border,
                thickness = 1.dp,
            )

            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = colors.textDim)) { append("≈ ") }
                        withStyle(
                            SpanStyle(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold,
                            ),
                        ) { append(fxPreview) }
                    },
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    text = "Rate locked at 1,042,000 IRR on save",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
                    color = colors.textFaint,
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                categoryOptions.forEach { (label, icon) ->
                    CategoryChip(
                        label = label,
                        icon = icon,
                        selected = label == category,
                        onClick = { category = label },
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            val timeNow = remember { LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetaRow(
                    icon = AramIcons.card,
                    label = "Cash",
                    value = "Melli",
                    modifier = Modifier.weight(1f),
                )
                MetaRow(
                    icon = AramIcons.calendar,
                    label = "Today",
                    value = timeNow,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(16.dp))

            Column(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Note",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = colors.textDim,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "(optional)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Normal,
                            letterSpacing = 0.sp,
                        ),
                        color = colors.textFaint,
                    )
                }
                Spacer(Modifier.height(6.dp))
                BasicTextField(
                    value = note,
                    onValueChange = { note = it.take(80) },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(colors.surface1)
                                .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            if (note.isEmpty()) {
                                Text(
                                    text = "Coffee with Nima",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = colors.textFaint,
                                )
                            }
                            innerTextField()
                        }
                    },
                )
            }

            Spacer(Modifier.height(16.dp))

            AramPrimaryButton(
                text = "Save expense",
                onClick = { onSave(parsedAmount, category, note) },
                enabled = parsedAmount > 0,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private const val IRR_PER_USD = 1_042_000L

private val categoryOptions = listOf(
    "Food" to AramIcons.cafe,
    "Transport" to AramIcons.car,
    "Bills" to AramIcons.receipt,
    "Shopping" to AramIcons.shoppingBag,
    "Fun" to AramIcons.smile,
)

@Composable
private fun CategoryChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
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
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) colors.primaryHi else colors.textFaint,
            modifier = Modifier.size(15.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.sp),
            color = if (selected) colors.primaryHi else colors.textDim,
        )
    }
}

@Composable
private fun MetaRow(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.aramColors
    Row(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface1)
            .border(1.dp, colors.border, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.textFaint,
            modifier = Modifier.size(15.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.sp,
            ),
            color = colors.textDim,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.sp,
            ),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun categoryIcon(category: String): ImageVector = when (category) {
    "Transport" -> AramIcons.car
    "Bills" -> AramIcons.receipt
    "Shopping" -> AramIcons.shoppingBag
    "Fun" -> AramIcons.smile
    else -> AramIcons.cafe
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun TransactionsScreenPreview() {
    AramTheme {
        TransactionsScreen()
    }
}
