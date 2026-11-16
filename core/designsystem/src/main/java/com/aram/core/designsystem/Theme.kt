package com.aram.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Tokens that Material 3 has no slot for: translucent glass layers, borders,
 * the dim/faint text ramp, and the gradient stops used by hero surfaces.
 */
@Immutable
data class AramColors(
    val textDim: Color,
    val textFaint: Color,
    val border: Color,
    val borderStrong: Color,
    val surface1: Color,
    val surface2: Color,
    val glassTint: Color,
    val glassHighlight: Color,
    val glassGradientEnd: Color,
    val tabBarTint: Color,
    val primaryHi: Color,
    val secondaryHi: Color,
    val tertiaryHi: Color,
    val heroStart: Color,
    val heroMid: Color,
    val heroEnd: Color,
    val heroDeep: Color,
    val chartStart: Color,
    val chartEnd: Color,
    val danger: Color,
    val dangerSoft: Color,
    val warningSoft: Color,
    val successSoft: Color,
    val infoSoft: Color,
    val scrim: Color,
)

private val darkAramColors = AramColors(
    textDim = Color(0xFFA3B3AC),
    textFaint = Color(0xFF7C8C85),
    border = Color(0x14FFFFFF),
    borderStrong = Color(0x29FFFFFF),
    surface1 = Color(0x0AFFFFFF),
    surface2 = Color(0x12FFFFFF),
    glassTint = Color(0x6B101C18),
    glassHighlight = Color(0x12FFFFFF),
    glassGradientEnd = Color(0x09FFFFFF),
    tabBarTint = Color(0x9E101C18),
    primaryHi = Color(0xFF2FD39A),
    secondaryHi = Color(0xFFFFB95C),
    tertiaryHi = Color(0xFFA8C7FA),
    heroStart = Color(0xFF10A874),
    heroMid = Color(0xFF0E9F6E),
    heroEnd = Color(0xFF0A7F5C),
    heroDeep = Color(0xFF065843),
    chartStart = Color(0xFF2FD39A),
    chartEnd = Color(0xFF0E9F6E),
    danger = Color(0xFFFFB4AB),
    dangerSoft = Color(0x1FBA1A1A),
    warningSoft = Color(0x24D97706),
    successSoft = Color(0x240E9F6E),
    infoSoft = Color(0x242563EB),
    scrim = Color(0x94030A07),
)

private val lightAramColors = AramColors(
    textDim = Color(0xFF3F4944),
    textFaint = Color(0xFF5B665F),
    border = Color(0x1410201A),
    borderStrong = Color(0x2910201A),
    surface1 = Color(0xA8FFFFFF),
    surface2 = Color(0xD1FFFFFF),
    glassTint = Color(0x8CFFFFFF),
    glassHighlight = Color(0xB3FFFFFF),
    glassGradientEnd = Color(0x8CFFFFFF),
    tabBarTint = Color(0xE6FFFFFF),
    primaryHi = Color(0xFF087A54),
    secondaryHi = Color(0xFF9A4B00),
    tertiaryHi = Color(0xFF1D4ED8),
    heroStart = Color(0xFF10A874),
    heroMid = Color(0xFF0E9F6E),
    heroEnd = Color(0xFF0A7F5C),
    heroDeep = Color(0xFF065843),
    chartStart = Color(0xFF2FD39A),
    chartEnd = Color(0xFF0E9F6E),
    danger = Color(0xFFBA1A1A),
    dangerSoft = Color(0x1FBA1A1A),
    warningSoft = Color(0x24D97706),
    successSoft = Color(0x240E9F6E),
    infoSoft = Color(0x242563EB),
    scrim = Color(0x94031007),
)

private val lightColors = lightColorScheme(
    primary = Color(0xFF0E9F6E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBCF0DA),
    onPrimaryContainer = Color(0xFF00281C),
    secondary = Color(0xFFD97706),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE3BE),
    onSecondaryContainer = Color(0xFF2B1700),
    tertiary = Color(0xFF2563EB),
    onTertiary = Color.White,
    background = Color(0xFFF7FAF8),
    onBackground = Color(0xFF10201A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF10201A),
    surfaceVariant = Color(0xFFEDF2EF),
    onSurfaceVariant = Color(0xFF3F4944),
    outline = Color(0xFF9AA8A1),
    outlineVariant = Color(0xFFD5DED9),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
)

private val darkColors = darkColorScheme(
    primary = Color(0xFF0E9F6E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF00513D),
    onPrimaryContainer = Color(0xFF8CF8D0),
    secondary = Color(0xFFD97706),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF633F00),
    onSecondaryContainer = Color(0xFFFFDDB7),
    tertiary = Color(0xFF2563EB),
    onTertiary = Color.White,
    background = Color(0xFF0B1512),
    onBackground = Color(0xFFE4EDE9),
    surface = Color(0xFF101C18),
    onSurface = Color(0xFFE4EDE9),
    surfaceVariant = Color(0xFF16241F),
    onSurfaceVariant = Color(0xFFA3B3AC),
    outline = Color(0xFF52635B),
    outlineVariant = Color(0xFF293A33),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

val AramShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

val LocalAramColors = staticCompositionLocalOf { darkAramColors }

val MaterialTheme.aramColors: AramColors
    @Composable
    @ReadOnlyComposable
    get() = LocalAramColors.current

@Composable
fun AramTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val scheme = if (darkTheme) darkColors else lightColors
    CompositionLocalProvider(
        LocalAramColors provides if (darkTheme) darkAramColors else lightAramColors,
        LocalContentColor provides scheme.onBackground,
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = AramTypography,
            shapes = AramShapes,
            content = content,
        )
    }
}
