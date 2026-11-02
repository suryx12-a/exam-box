package com.smartexambox.security.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Core Deep Navy & Cyber Security Colors
val NavyBackground = Color(0xFF090D1A)
val NavySurface = Color(0xFF10172A)
val NavySurfaceVariant = Color(0xFF19223D)
val NavySurfaceElevated = Color(0xFF1E294B)
val NavyBorder = Color(0xFF2B3A60)

// Accent Colors
val CyberCyan = Color(0xFF00E5FF)
val ElectricBlue = Color(0xFF38BDF8)
val DeepCyanContainer = Color(0xFF00384D)
val IndigoShield = Color(0xFF6366F1)
val PurpleVault = Color(0xFFA855F7)

// Status & Severity Colors
val StatusProtected = Color(0xFF10B981) // Emerald Green
val StatusWarning = Color(0xFFF59E0B)   // Amber Orange
val StatusCritical = Color(0xFFEF4444)  // Crimson Red
val StatusInfo = Color(0xFF06B6D4)      // Cyan Info

// Text & Content Colors
val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)
val TextMuted = Color(0xFF64748B)

// Gradients
val SecurityGradient = Brush.verticalGradient(
    colors = listOf(NavyBackground, Color(0xFF0C1326), Color(0xFF080D1C))
)

val CardGlowGradient = Brush.linearGradient(
    colors = listOf(NavySurfaceVariant.copy(alpha = 0.85f), NavySurface.copy(alpha = 0.95f))
)

val CyanAccentGradient = Brush.horizontalGradient(
    colors = listOf(CyberCyan, ElectricBlue)
)

val DangerGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFFDC2626), Color(0xFFEF4444))
)

val WarningGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFFD97706), Color(0xFFF59E0B))
)

val SuccessGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFF059669), Color(0xFF10B981))
)
