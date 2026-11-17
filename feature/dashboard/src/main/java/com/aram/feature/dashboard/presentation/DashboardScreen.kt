package com.aram.feature.dashboard.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aram.core.designsystem.AramTextStyles
import com.aram.core.designsystem.AramTheme
import com.aram.core.designsystem.aramColors
import com.aram.core.designsystem.component.AramBar
import com.aram.core.designsystem.component.AramBarTone
import com.aram.core.designsystem.component.AramChip
import com.aram.core.designsystem.component.AramChipTone
import com.aram.core.designsystem.component.AramIconButton
import com.aram.core.designsystem.component.AramRing
import com.aram.core.designsystem.component.AramSectionHeader
import com.aram.core.designsystem.component.GlassCard
import com.aram.core.designsystem.component.MoneyLabel
import com.aram.core.designsystem.icon.AramIcons

@Composable
fun DashboardRoute(
    displayName: String,
    modifier: Modifier = Modifier,
    onSeeAll: () -> Unit = {},
) {
    DashboardScreen(displayName = displayName, onSeeAll = onSeeAll, modifier = modifier)
}

@Composable
fun DashboardScreen(
    displayName: String,
    modifier: Modifier = Modifier,
    onSeeAll: () -> Unit = {},
) {
    val colors = MaterialTheme.aramColors
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column {
                Text(
                    text = "Welcome back,",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textDim,
                )
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
            AramIconButton(
                icon = AramIcons.notifications,
                contentDescription = "Notifications",
                onClick = {},
                modifier = Modifier.padding(top = 2.dp),
            )
        }

        SafeToSpendHero()

        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                label = "This month",
                value = "48,200,000",
                delta = "6.4% under Oct",
                deltaUp = false,
                modifier = Modifier.weight(1f),
            )
            StatTile(
                label = "Saved",
                value = "9,800,000",
                delta = "+1,200,000",
                deltaUp = true,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionTile(AramIcons.add, "Add", Modifier.weight(1f))
            ActionTile(AramIcons.wallet, "Income", Modifier.weight(1f))
            ActionTile(AramIcons.qr, "Scan", Modifier.weight(1f))
            ActionTile(AramIcons.target, "Goal", Modifier.weight(1f))
        }

        AramSectionHeader(
            title = "Budgets",
            action = "See all",
            onAction = onSeeAll,
            modifier = Modifier.padding(top = 8.dp),
        )

        GlassCard(shape = RoundedCornerShape(22.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AramRing(progress = 0.68f, size = 76, modifier = Modifier.padding(end = 16.dp))
                Column(Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = "October", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "68%",
                            style = AramTextStyles.statValue,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    BudgetLine(label = "Groceries", value = "4,120,000 / 6,000,000")
                    AramBar(progress = 0.68f, height = 8, modifier = Modifier.padding(top = 6.dp))
                    Spacer(Modifier.height(10.dp))
                    BudgetLine(label = "Dining out", value = "1,760,000 / 2,000,000")
                    AramBar(
                        progress = 0.88f,
                        tone = AramBarTone.Warning,
                        height = 8,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }

        AramSectionHeader(title = "Recent", action = "See all", onAction = onSeeAll)

        GlassCard(
            shape = RoundedCornerShape(22.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        ) {
            RecentRow(
                icon = AramIcons.cafe,
                title = "Café Nima",
                meta = "Food · 09:12",
                amount = -180_000,
            )
            RecentRow(
                icon = AramIcons.car,
                title = "Metro top-up",
                meta = "Transport · 08:04",
                amount = -500_000,
            )
            RecentRow(
                icon = AramIcons.trending,
                title = "Salary",
                meta = "Income · Oct 30",
                amount = 85_000_000,
                incoming = true,
                showDivider = false,
            )
        }

        Spacer(Modifier.height(12.dp))

        GlassCard(shape = RoundedCornerShape(22.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "This week", style = MaterialTheme.typography.titleMedium)
                AramChip(text = "−6.4%", tone = AramChipTone.Positive)
            }
            Spacer(Modifier.height(14.dp))
            WeekChart()
        }
    }
}

@Composable
private fun SafeToSpendHero() {
    val colors = MaterialTheme.aramColors
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(colors.heroStart, colors.heroMid, colors.heroEnd, colors.heroDeep),
                ),
            )
            .border(1.dp, Color(0x33FFFFFF), shape)
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        colorStops = arrayOf(0f to Color(0x6BFFFFFF), 0.46f to Color.Transparent),
                        center = Offset(size.width * 0.08f, 0f),
                        radius = size.width * 1.2f,
                    ),
                )
                drawCircle(
                    color = Color(0x17FFFFFF),
                    radius = 95.dp.toPx(),
                    center = Offset(size.width - 49.dp.toPx(), 49.dp.toPx()),
                    style = Stroke(width = 26.dp.toPx()),
                )
            }
            .padding(20.dp),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Safe to spend",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xDBFFFFFF),
                )
                HeroPill(text = "On track", icon = AramIcons.check)
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "12,400,000",
                    style = AramTextStyles.heroAmount,
                    color = Color.White,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "IRR",
                    style = MaterialTheme.typography.labelMedium,
                    letterSpacing = 1.sp,
                    color = Color(0xCCFFFFFF),
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = "≈ 412,000 a day for the next 30 days",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xD1FFFFFF),
            )
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HeroPill(text = "68% of budget", icon = AramIcons.trending)
                Spacer(Modifier.weight(1f))
                Text(
                    text = "22 days left",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xE6FFFFFF),
                )
            }
        }
    }
}

@Composable
private fun HeroPill(
    text: String,
    icon: ImageVector? = null,
) {
    Row(
        modifier = Modifier
            .height(30.dp)
            .clip(CircleShape)
            .background(Color(0x29FFFFFF))
            .border(1.dp, Color(0x42FFFFFF), CircleShape)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(text = text, style = MaterialTheme.typography.labelMedium, color = Color.White)
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    delta: String,
    deltaUp: Boolean,
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (deltaUp) AramIcons.chevronUp else AramIcons.chevronDown,
                contentDescription = null,
                tint = if (deltaUp) colors.primaryHi else colors.secondaryHi,
                modifier = Modifier.size(12.dp),
            )
            Text(
                text = delta,
                style = MaterialTheme.typography.labelSmall,
                color = if (deltaUp) colors.primaryHi else colors.secondaryHi,
            )
        }
    }
}

@Composable
private fun ActionTile(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.aramColors
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface1)
            .border(1.dp, colors.border, RoundedCornerShape(14.dp))
            .clickable {}
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = colors.primaryHi,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = colors.textDim,
        )
    }
}

@Composable
private fun BudgetLine(label: String, value: String) {
    val colors = MaterialTheme.aramColors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = colors.textDim,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            color = colors.textDim,
        )
    }
}

@Composable
private fun RecentRow(
    icon: ImageVector,
    title: String,
    meta: String,
    amount: Long,
    incoming: Boolean = false,
    showDivider: Boolean = true,
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
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.textDim,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = meta,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textFaint,
                )
            }
            MoneyLabel(amount = amount, incoming = incoming)
        }
        if (showDivider) {
            HorizontalDivider(color = colors.border, thickness = 1.dp)
        }
    }
}

@Composable
private fun WeekChart() {
    val colors = MaterialTheme.aramColors
    val bars = listOf(38, 62, 88, 54, 46, 30, 22)
    val labels = listOf("M", "T", "W", "T", "F", "S", "S")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        bars.forEachIndexed { index, value ->
            val isToday = index == 2
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(value / 100f)
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 3.dp, bottomEnd = 3.dp))
                        .background(
                            if (isToday) {
                                Brush.verticalGradient(listOf(Color(0xFFFFB95C), Color(0x80D97706)))
                            } else {
                                Brush.verticalGradient(listOf(Color(0xF22FD39A), Color(0x730E9F6E)))
                            },
                        ),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = labels[index],
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = colors.textFaint,
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun DashboardScreenPreview() {
    AramTheme {
        DashboardScreen(displayName = "Aram")
    }
}
