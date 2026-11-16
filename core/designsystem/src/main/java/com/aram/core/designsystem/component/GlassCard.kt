package com.aram.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.aram.core.designsystem.aramColors
import androidx.compose.material3.MaterialTheme

/**
 * The prototype's glass layer: a light-to-dark sheen, a translucent tint and a
 * hairline rim. It is a tinted fallback rather than a live backdrop blur, which
 * Compose cannot do below API 31.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.aramColors
    Column(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        colors.glassHighlight,
                        colors.glassGradientEnd,
                        colors.glassHighlight,
                    ),
                ),
            )
            .background(colors.glassTint)
            .border(1.dp, colors.border, shape)
            .padding(contentPadding),
        content = content,
    )
}
