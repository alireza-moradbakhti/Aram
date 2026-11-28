package com.aram.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aram.core.designsystem.AramTextStyles
import com.aram.core.designsystem.aramColors
import com.aram.core.designsystem.icon.AramIcons

@Composable
fun AramPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
) {
    Box(
        modifier = modifier
            .height(52.dp)
            .alpha(if (enabled) 1f else 0.42f)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF19B983), MaterialTheme.colorScheme.primary, Color(0xFF0A7D58)),
                ),
            )
            .clickable(enabled = enabled && !loading, onClick = onClick)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            androidx.compose.material3.CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}

@Composable
fun AramGlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    val colors = MaterialTheme.aramColors
    Row(
        modifier = modifier
            .height(52.dp)
            .clip(CircleShape)
            .background(colors.surface2)
            .border(1.dp, colors.border, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = colors.primaryHi, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
fun AramGhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    val colors = MaterialTheme.aramColors
    Row(
        modifier = modifier
            .height(52.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = colors.textDim, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text = text, style = MaterialTheme.typography.labelLarge, color = colors.textDim)
    }
}

@Composable
fun AramIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.aramColors
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(colors.surface1)
            .border(1.dp, colors.border, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = colors.textDim,
            modifier = Modifier.size(20.dp),
        )
    }
}

enum class AramChipTone { Neutral, Positive, Warning, Info, Danger }

@Composable
fun AramChip(
    text: String,
    modifier: Modifier = Modifier,
    tone: AramChipTone = AramChipTone.Neutral,
) {
    val colors = MaterialTheme.aramColors
    val (content, background, border) = when (tone) {
        AramChipTone.Neutral -> Triple(colors.textDim, colors.surface1, colors.border)
        AramChipTone.Positive -> Triple(colors.primaryHi, colors.successSoft, Color(0x470E9F6E))
        AramChipTone.Warning -> Triple(colors.secondaryHi, colors.warningSoft, Color(0x4DD97706))
        AramChipTone.Info -> Triple(colors.tertiaryHi, colors.infoSoft, Color(0x4D2563EB))
        AramChipTone.Danger -> Triple(colors.danger, colors.dangerSoft, Color(0x3DFFB4AB))
    }
    Box(
        modifier = modifier
            .height(30.dp)
            .clip(CircleShape)
            .background(background)
            .border(1.dp, border, CircleShape)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = MaterialTheme.typography.labelMedium, color = content)
    }
}

enum class AramBarTone { Positive, Warning, Danger }

@Composable
fun AramBar(
    progress: Float,
    modifier: Modifier = Modifier,
    tone: AramBarTone = AramBarTone.Positive,
    height: Int = 8,
) {
    val colors = MaterialTheme.aramColors
    val brush = when (tone) {
        AramBarTone.Positive -> Brush.horizontalGradient(listOf(Color(0xFF19B983), colors.chartEnd))
        AramBarTone.Warning -> Brush.horizontalGradient(listOf(Color(0xFFF59E0B), MaterialTheme.colorScheme.secondary))
        AramBarTone.Danger -> Brush.horizontalGradient(listOf(Color(0xFFF87171), Color(0xFFDC2626)))
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(CircleShape)
            .background(colors.surface2),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .clip(CircleShape)
                .background(brush),
        )
    }
}

@Composable
fun AramRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Int = 76,
    stroke: Float = 7f,
) {
    val colors = MaterialTheme.aramColors
    Canvas(modifier = modifier.size(size.dp)) {
        val strokeWidth = stroke.dp.toPx()
        val inset = strokeWidth / 2
        val arcSize = androidx.compose.ui.geometry.Size(
            this.size.width - strokeWidth,
            this.size.height - strokeWidth,
        )
        drawArc(
            color = colors.border,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
            size = arcSize,
            style = Stroke(width = strokeWidth),
        )
        drawArc(
            brush = Brush.linearGradient(listOf(colors.chartStart, colors.chartEnd)),
            startAngle = -90f,
            sweepAngle = 360f * progress.coerceIn(0f, 1f),
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )
    }
}

@Composable
fun AramSegmented(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.aramColors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(colors.surface1)
            .border(1.dp, colors.border, CircleShape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            val background by animateColorAsState(
                targetValue = if (selected) colors.surface2 else Color.Transparent,
                label = "segment",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(CircleShape)
                    .background(background)
                    .then(if (selected) Modifier.border(1.dp, colors.border, CircleShape) else Modifier)
                    .clickable { onSelect(index) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) MaterialTheme.colorScheme.onSurface else colors.textDim,
                )
            }
        }
    }
}

@Composable
fun AramSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.aramColors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        if (action != null) {
            Text(
                text = action,
                style = MaterialTheme.typography.labelMedium,
                color = colors.primaryHi,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(enabled = onAction != null) { onAction?.invoke() }
                    .padding(horizontal = 4.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
fun AramAppBar(
    title: String,
    modifier: Modifier = Modifier,
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 2.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.weight(1f),
        )
        actions()
    }
}

@Composable
fun AramMonthNav(
    title: String,
    subtitle: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    onCurrentMonth: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.aramColors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 12.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface1)
            .border(1.dp, colors.border, RoundedCornerShape(16.dp))
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NavArrow(
            icon = AramIcons.chevronLeft,
            contentDescription = "Previous month",
            onClick = onPrevious,
        )
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = colors.textFaint,
                textAlign = TextAlign.Center,
            )
        }
        if (onCurrentMonth != null) {
            Box(
                modifier = Modifier
                    .height(40.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onCurrentMonth)
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "This month",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.primaryHi,
                )
            }
        }
        NavArrow(
            icon = AramIcons.chevronRight,
            contentDescription = "Next month",
            onClick = onNext,
        )
    }
}

@Composable
private fun NavArrow(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.aramColors
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = colors.textDim,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
fun MoneyLabel(
    amount: Long,
    modifier: Modifier = Modifier,
    incoming: Boolean = false,
    fractionDigits: Int = 0,
) {
    val colors = MaterialTheme.aramColors
    Text(
        text = com.aram.core.designsystem.text.formatSignedMinor(amount, fractionDigits),
        style = AramTextStyles.money,
        color = if (incoming) colors.primaryHi else MaterialTheme.colorScheme.onSurface,
        modifier = modifier,
    )
}
