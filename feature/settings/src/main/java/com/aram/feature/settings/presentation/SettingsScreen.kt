package com.aram.feature.settings.presentation

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aram.core.designsystem.aramColors
import com.aram.core.designsystem.component.AramAppBar
import com.aram.core.designsystem.component.AramChip
import com.aram.core.designsystem.component.AramChipTone
import com.aram.core.designsystem.component.AramGhostButton
import com.aram.core.designsystem.component.AramIconButton
import com.aram.core.designsystem.component.AramPrimaryButton
import com.aram.core.designsystem.component.GlassCard
import com.aram.core.designsystem.icon.AramIcons

data class DebugScenarioOption(
    val id: String,
    val label: String,
    val isFailure: Boolean,
)

@Composable
fun SettingsScreen(
    displayName: String,
    onSignOut: () -> Unit,
    showDebugActions: Boolean = false,
    debugScenarios: List<DebugScenarioOption> = emptyList(),
    activeDebugScenarioId: String? = null,
    onSelectDebugScenario: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val colors = MaterialTheme.aramColors
    val versionName = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "—"
    }
    var showAccountSheet by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 24.dp),
    ) {
        AramAppBar(title = "Profile") {
            AramIconButton(
                icon = AramIcons.settings,
                contentDescription = "Manage account",
                onClick = { showAccountSheet = true },
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Avatar(displayName = displayName)
            Text(
                text = displayName,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 12.dp),
            )
            Text(
                text = "Local profile",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textDim,
                modifier = Modifier.padding(top = 2.dp),
            )
            Row(
                modifier = Modifier.padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                )
                Text(
                    text = "Synced just now · offline-first",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textFaint,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }

        ProfileStats(
            modifier = Modifier.padding(top = 20.dp),
        )

        SettingsGroup(
            label = "Account",
            modifier = Modifier.padding(top = 24.dp),
        ) {
            SettingRow(
                icon = AramIcons.profile,
                title = "Edit profile",
                subtitle = "Name, photo, home currency",
                onClick = { showAccountSheet = true },
            )
            RowDivider()
            SettingRow(
                icon = AramIcons.shield,
                title = "Security",
                subtitle = "Biometrics and app lock",
                trailing = { AramChip(text = "On", tone = AramChipTone.Positive) },
                onClick = { toast(context, "Security settings (demo)") },
            )
            RowDivider()
            SettingRow(
                icon = AramIcons.notifications,
                title = "Notifications",
                subtitle = "Budgets, goals, weekly recap",
                trailingText = "All",
                onClick = { toast(context, "Notification settings (demo)") },
            )
        }

        SettingsGroup(
            label = "Preferences",
            modifier = Modifier.padding(top = 24.dp),
        ) {
            SettingRow(
                icon = AramIcons.contrast,
                title = "Appearance",
                subtitle = "Follow the system or pick one",
                trailingText = "Dark",
                onClick = { toast(context, "Appearance picker (demo)") },
            )
            RowDivider()
            SettingRow(
                icon = AramIcons.payments,
                title = "Currency",
                subtitle = "Formatting follows the token",
                trailingText = "IRR",
                onClick = { toast(context, "Currency is locked to IRR (demo)") },
            )
            RowDivider()
            SettingRow(
                icon = AramIcons.download,
                title = "Export data",
                subtitle = "CSV, generated on-device",
                trailingText = "CSV",
                onClick = { toast(context, "Preparing CSV export…") },
            )
        }

        SettingsGroup(
            label = "Account access",
            modifier = Modifier.padding(top = 24.dp),
        ) {
            SettingRow(
                icon = AramIcons.logout,
                title = "Log out or delete account",
                subtitle = "Manage this device and its data",
                danger = true,
                onClick = { showAccountSheet = true },
            )
        }

        if (showDebugActions) {
            SettingsGroup(
                label = "Developer",
                modifier = Modifier.padding(top = 24.dp),
            ) {
                if (debugScenarios.isNotEmpty()) {
                    Text(
                        text = "Network scenario",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textDim,
                        modifier = Modifier.padding(start = 14.dp, top = 14.dp, bottom = 10.dp),
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(start = 14.dp, end = 14.dp, bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        debugScenarios.forEach { option ->
                            val selected = option.id == activeDebugScenarioId
                            AramChip(
                                text = option.label,
                                tone = when {
                                    selected && option.isFailure -> AramChipTone.Danger
                                    selected -> AramChipTone.Positive
                                    else -> AramChipTone.Neutral
                                },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { onSelectDebugScenario(option.id) },
                            )
                        }
                    }
                    RowDivider()
                }
                SettingRow(
                    icon = AramIcons.bug,
                    title = "Simulate crash",
                    subtitle = "Debug builds only",
                    onClick = { throw IllegalStateException("Debug crash from Settings") },
                )
            }
        }

        Text(
            text = "Aram $versionName · local build · data stays on this device",
            style = MaterialTheme.typography.labelSmall,
            color = colors.textFaint,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
        )
    }

    if (showAccountSheet) {
        AccountSheet(
            onDismiss = { showAccountSheet = false },
            onLogout = {
                showAccountSheet = false
                showLogoutDialog = true
            },
            onToast = { message -> toast(context, message) },
        )
    }

    if (showLogoutDialog) {
        LogoutDialog(
            onDismiss = { showLogoutDialog = false },
            onConfirm = {
                showLogoutDialog = false
                onSignOut()
            },
        )
    }
}

@Composable
private fun LogoutDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val colors = MaterialTheme.aramColors
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.colorScheme.surface,
                        ),
                    ),
                )
                .border(1.dp, colors.borderStrong, RoundedCornerShape(26.dp))
                .padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .background(Color(0x1F0E9F6E))
                    .border(1.dp, Color(0x4D0E9F6E), RoundedCornerShape(17.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = AramIcons.logout,
                    contentDescription = null,
                    tint = colors.primaryHi,
                    modifier = Modifier.size(24.dp),
                )
            }
            Text(
                text = "Log out of Aram?",
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp, lineHeight = 24.sp),
                modifier = Modifier.padding(top = 14.dp),
            )
            Text(
                text = "Your ledger stays on this device. You can sign back in any time with the same email.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 20.sp),
                color = colors.textDim,
                modifier = Modifier.padding(top = 8.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AramGhostButton(
                    text = "Cancel",
                    onClick = onDismiss,
                    modifier = Modifier.height(44.dp),
                )
                AramPrimaryButton(
                    text = "Log out",
                    onClick = onConfirm,
                    modifier = Modifier.height(44.dp),
                )
            }
        }
    }
}

@Composable
private fun Avatar(displayName: String) {
    val initials = displayName
        .trim()
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }

    Box(
        modifier = Modifier
            .size(88.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF5BE7B7), Color(0xFF0E9F6E), Color(0xFF076249)),
                ),
            )
            .border(3.dp, Color(0x1FFFFFFF), RoundedCornerShape(32.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials.ifBlank { "A" },
            fontSize = 30.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            letterSpacing = (-0.6).sp,
            color = Color(0xFF04231A),
        )
    }
}

@Composable
private fun ProfileStats(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.aramColors
    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(color = colors.border)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatCell(value = "128", label = "Transactions", modifier = Modifier.weight(1f))
            StatSeparator()
            StatCell(value = "6", label = "Budgets", modifier = Modifier.weight(1f))
            StatSeparator()
            StatCell(value = "3", label = "Goals", modifier = Modifier.weight(1f))
        }
        HorizontalDivider(color = colors.border)
    }
}

@Composable
private fun StatCell(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 18.sp,
                lineHeight = 24.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                fontFeatureSettings = "tnum",
            ),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.aramColors.textFaint,
        )
    }
}

@Composable
private fun StatSeparator() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(32.dp)
            .background(MaterialTheme.aramColors.border),
    )
}

@Composable
private fun SettingsGroup(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label.uppercase(),
            fontSize = 11.sp,
            letterSpacing = 0.9.sp,
            color = MaterialTheme.aramColors.textFaint,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            contentPadding = PaddingValues(0.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    danger: Boolean = false,
    trailingText: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = MaterialTheme.aramColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(if (danger) colors.dangerSoft else colors.surface2)
                .border(
                    1.dp,
                    if (danger) Color(0x42FFB4AB) else colors.border,
                    RoundedCornerShape(11.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (danger) colors.danger else colors.textDim,
                modifier = Modifier.size(17.dp),
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (danger) colors.danger else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = colors.textFaint,
                modifier = Modifier.padding(top = 1.dp),
            )
        }
        Row(
            modifier = Modifier.padding(start = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (trailing != null) {
                trailing()
            } else {
                if (trailingText != null) {
                    Text(
                        text = trailingText,
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textFaint,
                    )
                    Spacer(Modifier.width(6.dp))
                }
                Icon(
                    imageVector = AramIcons.chevronRight,
                    contentDescription = null,
                    tint = colors.textFaint,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        color = MaterialTheme.aramColors.border,
        modifier = Modifier.padding(start = 60.dp),
    )
}

@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
private fun AccountSheet(
    onDismiss: () -> Unit,
    onLogout: () -> Unit,
    onToast: (String) -> Unit,
) {
    val colors = MaterialTheme.aramColors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .size(width = 38.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(colors.borderStrong),
            )
        },
    ) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 28.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Manage account",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                AramIconButton(
                    icon = AramIcons.close,
                    contentDescription = "Close",
                    onClick = onDismiss,
                )
            }
            Spacer(Modifier.height(8.dp))
            SheetRow(
                icon = AramIcons.edit,
                title = "Edit profile",
                onClick = { onToast("Edit profile (demo)") },
            )
            SheetRow(
                icon = AramIcons.key,
                title = "Change password",
                onClick = { onToast("Change password (demo)") },
            )
            SheetRow(
                icon = AramIcons.download,
                title = "Export my data",
                onClick = { onToast("Preparing CSV export…") },
            )
            SheetRow(
                icon = AramIcons.logout,
                title = "Log out",
                danger = true,
                onClick = onLogout,
            )
            SheetRow(
                icon = AramIcons.trash,
                title = "Delete account",
                danger = true,
                divider = false,
                onClick = { onToast("Delete account (demo — arrives with sync)") },
            )
        }
    }
}

@Composable
private fun SheetRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    danger: Boolean = false,
    divider: Boolean = true,
) {
    val colors = MaterialTheme.aramColors
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .clickable(onClick = onClick)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (danger) colors.danger else colors.textDim,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (danger) colors.danger else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp),
            )
            if (!danger) {
                Icon(
                    imageVector = AramIcons.chevronRight,
                    contentDescription = null,
                    tint = colors.textFaint,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        if (divider) {
            HorizontalDivider(color = colors.border, thickness = 1.dp)
        }
    }
}

private fun toast(context: android.content.Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}
