package com.aram.feature.transactions.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aram.core.designsystem.AramTheme
import com.aram.core.designsystem.aramColors
import com.aram.core.designsystem.component.AmountTextField
import com.aram.core.designsystem.component.AramIconButton
import com.aram.core.designsystem.component.AramPrimaryButton
import com.aram.core.designsystem.component.AramSegmented
import com.aram.core.designsystem.icon.AramIcons
import com.aram.core.designsystem.state.SubmitState
import com.aram.core.domain.transaction.TransactionField
import com.aram.core.model.Account
import com.aram.core.model.AccountType
import com.aram.core.model.Category
import com.aram.core.model.CategoryKind
import com.aram.core.model.TransactionType
import com.aram.core.money.CurrencyCode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionRoute(
    onClose: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddTransactionViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colors = MaterialTheme.aramColors

    LaunchedEffect(state.saved) {
        if (state.saved) onSaved()
    }

    ModalBottomSheet(
        onDismissRequest = onClose,
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
        modifier = modifier,
    ) {
        AddTransactionScreen(
            state = state,
            onTypeChange = viewModel::onTypeChange,
            onAmountChange = viewModel::onAmountChange,
            onAccountSelect = viewModel::onAccountSelected,
            onCategorySelect = viewModel::onCategorySelected,
            onDateSelect = viewModel::onDateSelected,
            onNoteChange = viewModel::onNoteChange,
            onSave = viewModel::onSave,
            onClose = onClose,
        )
    }
}

@Composable
fun AddTransactionScreen(
    state: AddTransactionUiState,
    onTypeChange: (TransactionType) -> Unit,
    onAmountChange: (String) -> Unit,
    onAccountSelect: (String) -> Unit,
    onCategorySelect: (String) -> Unit,
    onDateSelect: (LocalDate) -> Unit,
    onNoteChange: (String) -> Unit,
    onSave: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.aramColors
    var accountPickerOpen by rememberSaveable { mutableStateOf(false) }
    var datePickerOpen by rememberSaveable { mutableStateOf(false) }
    val isExpense = state.type == TransactionType.EXPENSE

    Column(
        modifier = modifier
            .fillMaxWidth()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (isExpense) "Add expense" else "Add income",
                style = MaterialTheme.typography.titleMedium,
            )
            AramIconButton(
                icon = AramIcons.close,
                contentDescription = "Close",
                onClick = onClose,
            )
        }

        Spacer(Modifier.height(12.dp))

        AramSegmented(
            options = listOf("Expense", "Income"),
            selectedIndex = if (isExpense) 0 else 1,
            onSelect = { index ->
                onTypeChange(if (index == 0) TransactionType.EXPENSE else TransactionType.INCOME)
            },
        )

        Spacer(Modifier.height(14.dp))

        AmountTextField(
            value = state.amountInput,
            onValueChange = onAmountChange,
            currency = state.currency,
            label = "Amount",
            error = state.amountError?.message(),
            enabled = state.submitState !is SubmitState.Submitting,
            large = true,
        )

        Spacer(Modifier.height(14.dp))

        if (state.visibleCategories.isEmpty()) {
            Text(
                text = if (state.optionsLoaded) {
                    "No ${if (isExpense) "expense" else "income"} categories yet."
                } else {
                    "Loading categories…"
                },
                style = MaterialTheme.typography.bodySmall,
                color = colors.textFaint,
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.visibleCategories.forEach { category ->
                    CategoryChip(
                        label = category.name,
                        icon = categoryIcon(category.iconKey),
                        selected = category.id == state.selectedCategoryId,
                        onClick = { onCategorySelect(category.id) },
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        MetaRow(
            icon = AramIcons.wallet,
            label = "Account",
            value = state.selectedAccount?.let { "${it.name} · ${it.currencyCode}" } ?: "None",
            trailing = if (accountPickerOpen) AramIcons.chevronUp else AramIcons.chevronDown,
            onClick = { accountPickerOpen = !accountPickerOpen },
        )
        AnimatedVisibility(visible = accountPickerOpen) {
            AccountPicker(
                accounts = state.accounts,
                selectedId = state.selectedAccountId,
                onSelect = {
                    accountPickerOpen = false
                    onAccountSelect(it)
                },
            )
        }

        Spacer(Modifier.height(8.dp))

        MetaRow(
            icon = AramIcons.calendar,
            label = "Date",
            value = state.occurredOn.label(),
            trailing = null,
            onClick = { datePickerOpen = true },
        )

        val fieldMessage = when {
            TransactionField.ACCOUNT in state.invalidFields -> "Choose an account to save."
            TransactionField.CATEGORY in state.invalidFields -> "Choose a category to save."
            else -> null
        }
        if (fieldMessage != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = fieldMessage,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = colors.danger,
            )
        }

        Spacer(Modifier.height(16.dp))

        NoteField(
            value = state.note,
            onValueChange = onNoteChange,
            enabled = state.submitState !is SubmitState.Submitting,
        )

        if (state.submitState is SubmitState.Failed) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Couldn't save this ${if (isExpense) "expense" else "income"}. Try again.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.danger,
            )
        }

        Spacer(Modifier.height(16.dp))

        AramPrimaryButton(
            text = if (isExpense) "Save expense" else "Save income",
            onClick = onSave,
            enabled = state.canSave,
            loading = state.submitState is SubmitState.Submitting,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    if (datePickerOpen) {
        TransactionDatePicker(
            initial = state.occurredOn,
            onDismiss = { datePickerOpen = false },
            onConfirm = { date ->
                datePickerOpen = false
                onDateSelect(date)
            },
        )
    }
}

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
    trailing: ImageVector?,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.aramColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface1)
            .border(1.dp, colors.border, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
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
        if (trailing != null) {
            Spacer(Modifier.width(6.dp))
            Icon(
                imageVector = trailing,
                contentDescription = null,
                tint = colors.textFaint,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun AccountPicker(
    accounts: List<Account>,
    selectedId: String?,
    onSelect: (String) -> Unit,
) {
    val colors = MaterialTheme.aramColors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface1)
            .border(1.dp, colors.border, RoundedCornerShape(14.dp))
            .padding(vertical = 4.dp),
    ) {
        if (accounts.isEmpty()) {
            Text(
                text = "No accounts available.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textFaint,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            )
            return@Column
        }
        accounts.forEachIndexed { index, account ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(account.id) }
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = account.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "${account.type.label()} · ${account.currencyCode}",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
                        color = colors.textFaint,
                    )
                }
                if (account.id == selectedId) {
                    Icon(
                        imageVector = AramIcons.check,
                        contentDescription = null,
                        tint = colors.primaryHi,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            if (index != accounts.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    color = colors.border,
                    thickness = 1.dp,
                )
            }
        }
    }
}

@Composable
private fun NoteField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
) {
    val colors = MaterialTheme.aramColors
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
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
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
                    if (value.isEmpty()) {
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionDatePicker(
    initial: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
) {
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = initial.toPickerMillis(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val millis = pickerState.selectedDateMillis
                    if (millis != null) {
                        onConfirm(
                            Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate(),
                        )
                    } else {
                        onDismiss()
                    }
                },
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    ) {
        DatePicker(state = pickerState)
    }
}

private fun AmountInputError.message(): String = when (this) {
    AmountInputError.Empty -> "Enter an amount."
    AmountInputError.Zero -> "Amount must be greater than zero."
    AmountInputError.Invalid -> "Enter a valid amount."
    is AmountInputError.TooManyFractionDigits -> "Use at most $max decimal places."
}

private fun LocalDate.label(): String {
    val today = LocalDate.now()
    return when (this) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))
    }
}

private fun LocalDate.toPickerMillis(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun AccountType.label(): String = when (this) {
    AccountType.CASH -> "Cash"
    AccountType.BANK -> "Bank"
    AccountType.WALLET -> "Wallet"
    AccountType.SAVINGS -> "Savings"
    AccountType.INVESTMENT -> "Investment"
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun AddTransactionScreenDarkPreview() {
    AramTheme(darkTheme = true) {
        AddTransactionScreen(
            state = previewState(),
            onTypeChange = {},
            onAmountChange = {},
            onAccountSelect = {},
            onCategorySelect = {},
            onDateSelect = {},
            onNoteChange = {},
            onSave = {},
            onClose = {},
            modifier = Modifier.background(MaterialTheme.colorScheme.surface),
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun AddTransactionScreenLightPreview() {
    AramTheme(darkTheme = false) {
        AddTransactionScreen(
            state = previewState(amountInput = "12.345"),
            onTypeChange = {},
            onAmountChange = {},
            onAccountSelect = {},
            onCategorySelect = {},
            onDateSelect = {},
            onNoteChange = {},
            onSave = {},
            onClose = {},
            modifier = Modifier.background(MaterialTheme.colorScheme.surface),
        )
    }
}

private fun previewState(amountInput: String = "18.50") = AddTransactionUiState(
    amountInput = amountInput,
    amountError = if (amountInput == "12.345") {
        AmountInputError.TooManyFractionDigits(2)
    } else {
        null
    },
    currency = CurrencyCode.USD,
    accounts = listOf(
        Account(
            id = "cash",
            name = "Cash",
            type = AccountType.CASH,
            currencyCode = "USD",
            openingBalanceMinor = 0L,
            colorArgb = 0xFF0E9F6E.toInt(),
            iconKey = "wallet",
            archived = false,
        ),
    ),
    selectedAccountId = "cash",
    categories = listOf(
        Category(
            id = "food",
            name = "Food & Drinks",
            kind = CategoryKind.EXPENSE,
            parentId = null,
            colorArgb = 0xFFEF5350.toInt(),
            iconKey = "food",
            archived = false,
        ),
        Category(
            id = "salary",
            name = "Salary",
            kind = CategoryKind.INCOME,
            parentId = null,
            colorArgb = 0xFF059669.toInt(),
            iconKey = "salary",
            archived = false,
        ),
    ),
    selectedCategoryId = "food",
    optionsLoaded = true,
)
