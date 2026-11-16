package com.aram.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aram.core.designsystem.aramColors
import com.aram.core.designsystem.icon.AramIcons

/** Radial brand wash behind every main screen, matching the prototype's `.screen`. */
@Composable
fun AramBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f
    val green = scheme.primary
    val blue = scheme.tertiary
    val amber = scheme.secondary
    val greenAlpha = if (isDark) 0.22f else 0.16f
    val blueAlpha = if (isDark) 0.18f else 0.12f
    val amberAlpha = if (isDark) 0.13f else 0.10f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.background)
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(green.copy(alpha = greenAlpha), Color.Transparent),
                        center = Offset(size.width * 0.12f, 0f),
                        radius = size.width * 1.15f,
                    ),
                )
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(blue.copy(alpha = blueAlpha), Color.Transparent),
                        center = Offset(size.width, size.height * 0.08f),
                        radius = size.width * 0.9f,
                    ),
                )
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(amber.copy(alpha = amberAlpha), Color.Transparent),
                        center = Offset(size.width * 0.5f, size.height * 1.08f),
                        radius = size.width * 1.0f,
                    ),
                )
            },
        content = content,
    )
}

data class AramTab(
    val label: String,
    val icon: ImageVector,
)

@Composable
fun AramBottomBar(
    tabs: List<AramTab>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.aramColors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 14.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(colors.tabBarTint)
            .border(1.dp, colors.border, RoundedCornerShape(24.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEachIndexed { index, tab ->
            val selected = index == selectedIndex
            Column(
                modifier = Modifier
                    .widthIn(min = 62.dp)
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (selected) colors.successSoft else Color.Transparent)
                    .then(
                        if (selected) {
                            Modifier.border(1.dp, Color(0x380E9F6E), RoundedCornerShape(18.dp))
                        } else {
                            Modifier
                        },
                    )
                    .clickable { onSelect(index) }
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = tab.icon,
                    contentDescription = tab.label,
                    tint = if (selected) colors.primaryHi else colors.textFaint,
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    text = tab.label,
                    color = if (selected) colors.primaryHi else colors.textFaint,
                    fontSize = 10.sp,
                    letterSpacing = 0.3.sp,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
fun AramFab(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = AramIcons.add,
) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = modifier
            .size(56.dp)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF2FD39A), MaterialTheme.colorScheme.primary),
                ),
            )
            .border(1.dp, Color(0x3DFFFFFF), shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color(0xFF04140D),
            modifier = Modifier.size(24.dp),
        )
    }
}

/** A round, glassy mark used for the app identity and avatars. */
@Composable
fun AramMark(
    modifier: Modifier = Modifier,
    size: Int = 46,
    corner: Int = 15,
    text: String? = null,
) {
    val shape = RoundedCornerShape(corner.dp)
    Box(
        modifier = modifier
            .size(size.dp)
            .shadow(
                elevation = 14.dp,
                shape = shape,
                ambientColor = MaterialTheme.colorScheme.primary,
                spotColor = MaterialTheme.colorScheme.primary,
            )
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF5BE7B7), MaterialTheme.colorScheme.primary, Color(0xFF076249)),
                ),
            )
            .border(1.dp, Color(0x4DFFFFFF), shape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text ?: "A",
            color = Color(0xFF052018),
            fontSize = (size * 0.46f).sp,
            style = MaterialTheme.typography.headlineSmall,
        )
    }
}
