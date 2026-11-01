package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val UnisocDarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color(0xFF00363D),
    primaryContainer = Color(0xFF004F58),
    onPrimaryContainer = Color(0xFF97F0FF),
    secondary = ElectricBlue,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF00419C),
    onSecondaryContainer = Color(0xFFD9E2FF),
    tertiary = CyberViolet,
    onTertiary = Color(0xFFFFFFFF),
    background = CyberBg,
    onBackground = TextPrimary,
    surface = CyberSurface,
    onSurface = TextPrimary,
    surfaceVariant = CyberSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = CyberSurfaceBorder,
    error = StatusDanger,
    onError = Color.White
)

@Composable
fun UnisocTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = UnisocDarkColorScheme,
        typography = Typography,
        content = content
    )
}
