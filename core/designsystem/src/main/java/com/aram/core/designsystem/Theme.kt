package com.aram.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

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
    surfaceVariant = Color(0xFFE3EAE6),
    onSurfaceVariant = Color(0xFF3F4944),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
)

private val darkColors = darkColorScheme(
    primary = Color(0xFF6FDDB5),
    onPrimary = Color(0xFF00382A),
    primaryContainer = Color(0xFF00513D),
    onPrimaryContainer = Color(0xFF8CF8D0),
    secondary = Color(0xFFFFB95C),
    onSecondary = Color(0xFF452B00),
    secondaryContainer = Color(0xFF633F00),
    onSecondaryContainer = Color(0xFFFFDDB7),
    tertiary = Color(0xFFA8C7FA),
    onTertiary = Color(0xFF00315C),
    background = Color(0xFF0B1512),
    onBackground = Color(0xFFDCE5E0),
    surface = Color(0xFF101C18),
    onSurface = Color(0xFFDCE5E0),
    surfaceVariant = Color(0xFF3F4A45),
    onSurfaceVariant = Color(0xFFBEC9C3),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

@Composable
fun AramTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) darkColors else lightColors,
        typography = AramTypography,
        content = content,
    )
}
