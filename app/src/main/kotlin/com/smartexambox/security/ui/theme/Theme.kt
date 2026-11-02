package com.smartexambox.security.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SecurityDarkColorScheme = darkColorScheme(
    primary = CyberCyan,
    onPrimary = NavyBackground,
    primaryContainer = DeepCyanContainer,
    onPrimaryContainer = CyberCyan,
    secondary = ElectricBlue,
    onSecondary = NavyBackground,
    secondaryContainer = NavySurfaceVariant,
    onSecondaryContainer = ElectricBlue,
    tertiary = IndigoShield,
    onTertiary = TextPrimary,
    background = NavyBackground,
    onBackground = TextPrimary,
    surface = NavySurface,
    onSurface = TextPrimary,
    surfaceVariant = NavySurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = NavyBorder,
    error = StatusCritical,
    onError = TextPrimary,
    errorContainer = Color(0xFF450A0A),
    onErrorContainer = StatusCritical
)

@Composable
fun SmartExamSecurityTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SecurityDarkColorScheme,
        typography = Typography,
        content = content
    )
}
