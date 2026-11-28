package com.aram.feature.dashboard.presentation

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aram.core.designsystem.AramTextStyles
import com.aram.core.designsystem.AramTheme
import com.aram.core.designsystem.aramColors
import com.aram.core.designsystem.component.AramBar
import com.aram.core.designsystem.component.AramBarTone
import com.aram.core.designsystem.component.AramChip
import com.aram.core.designsystem.component.AramChipTone
import com.aram.core.designsystem.component.AramIconButton
import com.aram.core.designsystem.component.AramPrimaryButton
import com.aram.core.designsystem.component.AramRing
import com.aram.core.designsystem.component.AramSectionHeader
import com.aram.core.designsystem.component.GlassCard
import com.aram.core.designsystem.component.MoneyLabel
import com.aram.core.designsystem.icon.AramIcons
import com.aram.core.designsystem.text.formatMinor
import com.aram.core.designsystem.text.formatSignedMinor
import com.aram.core.domain.safetospend.SafeToSpend
import com.aram.core.domain.safetospend.SafeToSpendBreakdown
import com.aram.core.money.CurrencyCode
import com.aram.core.money.Money
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs

private enum class DashboardPhase { Loading, Error, Content }

@Composable
fun DashboardRoute(
    displayName: String,
    modifier: Modifier = Modifier,
    onSeeAll: () -> Unit = {},
    onExplainSafeToSpend: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DashboardScreen(
        displayName = displayName,
        state = state,
        onExplainSafeToSpend = onExplainSafeToSpend,
        onSeeAll = onSeeAll,
        onRetry = viewModel::retry,
        modifier = modifier,
    )
}

@Composable
fun DashboardScreen(
    displayName: String,
    state: DashboardUiState,
    modifier: Modifier = Modifier,
    onExplainSafeToSpend: () -> Unit = {},
    onSeeAll: () -> Unit = {},
    onRetry: () -> Unit = {},
) {
    val phase = when {
        state.isLoading -> DashboardPhase.Loading
        state.hasError -> DashboardPhase.Error
        else -> DashboardPhase.Content
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 96.dp),
    ) {
        Crossfade(
            targetState = phase,
            animationSpec = tween(durationMillis = 320),
            label = "dashboard-load",
        ) { current ->
            when (current) {
                DashboardPhase.Loading ->
                    DashboardSkeleton(modifier = Modifier.testTag(SKELETON_TAG))

                DashboardPhase.Error -> DashboardError(onRetry = onRetry)

                DashboardPhase.Content -> DashboardContent(
                    displayName = displayName,
                    state = state,
                    onExplainSafeToSpend = onExplainSafeToSpend,
                    onSeeAll = onSeeAll,
                )
            }
        }
    }
}

@Composable
private fun DashboardContent(
    displayName: String,
    state: DashboardUiState,
    onExplainSafeToSpend: () -> Unit,
    onSeeAll: () -> Unit,
) {
    val colors = MaterialTheme.aramColors
    val currency = state.spent?.currency ?: CurrencyCode.IRR
    val fractionDigits = currency.fractionDigits
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(76.dp),
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

        SafeToSpendHero(
            result = state.safeToSpend,
            onClick = onExplainSafeToSpend,
        )

        Spacer(Modifier.height(16.dp))

        val spent = state.spent ?: Money.zero(currency)
        val saved = state.saved ?: Money.zero(currency)
        val spentDelta = spentDeltaLabel(state, currency)
        val savedDelta = state.previousSaved
            ?.let { previous -> saved.amountMinor - previous.amountMinor }
            ?.takeIf { state.previousSaved != null }
            ?.let { delta -> formatSignedMinor(delta, fractionDigits) }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                label = "This month",
                value = formatMinor(spent.amountMinor, fractionDigits),
                delta = spentDelta,
                deltaUp = state.previousSpent == null || spent <= state.previousSpent,
                modifier = Modifier.weight(1f),
            )
            StatTile(
                label = "Saved",
                value = formatMinor(saved.amountMinor, fractionDigits),
                delta = savedDelta,
                deltaUp = (savedDelta?.startsWith("\u2212") != true),
                modifier = Modifier.weight(1f),
            )
        }

        if (state.isEmpty) {
            Spacer(Modifier.height(12.dp))
            EmptyFirstRunCard()
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

private fun spentDeltaLabel(state: DashboardUiState, currency: CurrencyCode): String? {
    val spent = state.spent ?: return null
    val previous = state.previousSpent ?: return null
    if (previous.amountMinor <= 0) return null
    val monthLabel = state.month
        ?.minusMonths(1)
        ?.month
        ?.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        ?: return null
    val diff = previous.amountMinor - spent.amountMinor
    val percent = abs(diff) * 100.0 / previous.amountMinor
    val direction = if (diff >= 0) "under" else "over"
    val percentLabel = String.format(
        Locale.US,
        if (percent >= 10) "%.0f" else "%.1f",
        percent,
    )
    return "$percentLabel% $direction $monthLabel"
}

@Composable
private fun EmptyFirstRunCard() {
    val colors = MaterialTheme.aramColors
    GlassCard(shape = RoundedCornerShape(22.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.surface2)
                    .border(1.dp, colors.border, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = AramIcons.add,
                    contentDescription = null,
                    tint = colors.primaryHi,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "No activity this month",
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = "Add your first expense to unlock your safe-to-spend.",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textFaint,
                )
            }
        }
    }
}

@Composable
private fun DashboardError(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.aramColors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 96.dp, bottom = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(colors.surface1)
                .border(1.dp, colors.border, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = AramIcons.notifications,
                contentDescription = null,
                tint = colors.textDim,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Couldn't load your dashboard",
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Check your connection and try again.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textDim,
        )
        Spacer(Modifier.height(20.dp))
        AramPrimaryButton(text = "Try again", onClick = onRetry)
    }
}

@Composable
private fun DashboardSkeleton(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(76.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column {
                SkeletonBlock(
                    modifier = Modifier
                        .width(92.dp)
                        .height(11.dp),
                    shape = RoundedCornerShape(6.dp),
                )
                Spacer(Modifier.height(10.dp))
                SkeletonBlock(
                    modifier = Modifier
                        .width(128.dp)
                        .height(22.dp),
                    shape = RoundedCornerShape(7.dp),
                )
            }
            SkeletonBlock(modifier = Modifier.size(40.dp), shape = CircleShape)
        }

        SkeletonBlock(
            modifier = Modifier
                .fillMaxWidth()
                .height(188.dp),
            shape = RoundedCornerShape(28.dp),
        )

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            repeat(2) {
                SkeletonBlock(
                    modifier = Modifier
                        .weight(1f)
                        .height(100.dp),
                    shape = RoundedCornerShape(18.dp),
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            repeat(4) {
                SkeletonBlock(
                    modifier = Modifier
                        .weight(1f)
                        .height(66.dp),
                    shape = RoundedCornerShape(14.dp),
                )
            }
        }

        SkeletonSectionHeader(extraTop = 8.dp, widthFraction = 0.34f)
        SkeletonBlock(
            modifier = Modifier
                .fillMaxWidth()
                .height(116.dp),
            shape = RoundedCornerShape(22.dp),
        )
        SkeletonSectionHeader(widthFraction = 0.28f)
        SkeletonBlock(
            modifier = Modifier
                .fillMaxWidth()
                .height(214.dp),
            shape = RoundedCornerShape(22.dp),
        )
    }
}

@Composable
private fun SkeletonSectionHeader(
    extraTop: Dp = 0.dp,
    widthFraction: Float,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp + extraTop, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SkeletonBlock(
            modifier = Modifier
                .fillMaxWidth(widthFraction)
                .height(24.dp),
            shape = RoundedCornerShape(7.dp),
        )
    }
}

@Composable
private fun SkeletonBlock(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(10.dp),
) {
    val colors = MaterialTheme.aramColors
    Box(
        modifier
            .clip(shape)
            .background(colors.surface1)
            .border(1.dp, colors.border, shape)
            .shimmerBand(),
    )
}

@Composable
private fun Modifier.shimmerBand(): Modifier {
    // Keyed to the theme, not the system setting: AramTheme can force either
    // scheme (settings override, previews, screenshot tests).
    val darkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val highlight = if (darkTheme) {
        Color.White.copy(alpha = 0.09f)
    } else {
        Color.Black.copy(alpha = 0.045f)
    }
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerProgress",
    )
    return drawWithContent {
        drawContent()
        val bandWidth = size.width * 0.55f
        val startX = -bandWidth + (size.width + bandWidth) * progress
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, highlight, Color.Transparent),
                startX = startX,
                endX = startX + bandWidth,
            ),
        )
    }
}

internal const val SKELETON_TAG = "dashboard-skeleton"
internal const val HERO_TAG = "dashboard-safe-to-spend"

@Composable
private fun SafeToSpendHero(
    result: SafeToSpend?,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.aramColors
    val shape = RoundedCornerShape(28.dp)
    val total = result?.total ?: Money.zero(CurrencyCode.IRR)
    val fractionDigits = total.currency.fractionDigits
    val onTrack = total.amountMinor > 0
    val daysRemaining = result?.daysRemaining ?: 1
    val daily = result?.dailyAmount ?: total
    val incomeMinor = result?.breakdown?.income?.amountMinor ?: 0L
    val spentMinor = result?.breakdown?.spent?.amountMinor?.let { -it } ?: 0L
    val incomeShare = if (incomeMinor > 0) {
        ((spentMinor * 100.0) / incomeMinor).toInt().coerceIn(0, 100)
    } else {
        null
    }
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
            .clickable(onClick = onClick)
            .padding(20.dp)
            .testTag(HERO_TAG),
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
                HeroPill(
                    text = if (onTrack) "On track" else "At limit",
                    icon = AramIcons.check,
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = formatMinor(total.amountMinor, fractionDigits),
                    style = AramTextStyles.heroAmount,
                    color = Color.White,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = total.currency.isoCode,
                    style = MaterialTheme.typography.labelMedium,
                    letterSpacing = 1.sp,
                    color = Color(0xCCFFFFFF),
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = "\u2248 ${formatMinor(daily.amountMinor, fractionDigits)} a day for the next " +
                    if (daysRemaining == 1) "day" else "$daysRemaining days",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xD1FFFFFF),
            )
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (incomeShare != null) {
                    HeroPill(text = "$incomeShare% of income", icon = AramIcons.trending)
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = if (daysRemaining == 1) "1 day left" else "$daysRemaining days left",
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
    delta: String?,
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
        if (delta != null) {
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
private fun DashboardContentPreview() {
    AramTheme(darkTheme = false) {
        DashboardScreen(displayName = "Ava Bennett", state = sampleDashboardState())
    }
}

@Preview(name = "Content · dark", showBackground = true, showSystemUi = true)
@Composable
private fun DashboardContentDarkPreview() {
    AramTheme(darkTheme = true) {
        DashboardScreen(displayName = "Ava Bennett", state = sampleDashboardState())
    }
}

@Preview(name = "Empty · light", showBackground = true, showSystemUi = true)
@Composable
private fun DashboardEmptyPreview() {
    AramTheme(darkTheme = false) {
        DashboardScreen(
            displayName = "Ava Bennett",
            state = sampleDashboardState().copy(
                isEmpty = true,
                spent = Money.zero(CurrencyCode.IRR),
                saved = Money.zero(CurrencyCode.IRR),
                previousSpent = null,
                previousSaved = null,
            ),
        )
    }
}

@Preview(name = "Error · light", showBackground = true, showSystemUi = true)
@Composable
private fun DashboardErrorPreview() {
    AramTheme(darkTheme = false) {
        DashboardScreen(
            displayName = "Ava Bennett",
            state = DashboardUiState(isLoading = false, hasError = true),
        )
    }
}

@Preview(name = "Loading · light", showBackground = true, showSystemUi = true)
@Composable
private fun DashboardSkeletonLightPreview() {
    AramTheme(darkTheme = false) {
        DashboardScreen(displayName = "Ava Bennett", state = DashboardUiState(isLoading = true))
    }
}

@Preview(name = "Loading · dark", showBackground = true, showSystemUi = true)
@Composable
private fun DashboardSkeletonDarkPreview() {
    AramTheme(darkTheme = true) {
        DashboardScreen(displayName = "Ava Bennett", state = DashboardUiState(isLoading = true))
    }
}

private fun sampleDashboardState(): DashboardUiState {
    val month = YearMonth.of(2026, 11)
    val currency = CurrencyCode.IRR
    val income = Money(85_000_000, currency)
    val spent = Money(48_200_000, currency)
    val total = Money(36_800_000, currency)
    return DashboardUiState(
        isLoading = false,
        month = month,
        safeToSpend = SafeToSpend(
            total = total,
            daysRemaining = 15,
            dailyAmount = Money(2_453_333, currency),
            breakdown = SafeToSpendBreakdown(
                income = income,
                spent = Money(-spent.amountMinor, currency),
                plannedBudgets = Money.zero(currency),
                upcomingBills = Money.zero(currency),
                clampAdjustment = Money.zero(currency),
                total = total,
            ),
        ),
        spent = spent,
        saved = income - spent,
        previousSpent = Money(51_500_000, currency),
        previousSaved = Money(33_500_000, currency),
    )
}
