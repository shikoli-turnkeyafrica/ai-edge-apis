package com.google.sample.fcdemo.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// CashFlow Modern Financial Theme Colors
val CashFlowPrimary = Color(0xFF00C853)        // Bright Green
val CashFlowPrimaryDark = Color(0xFF00A844)    // Dark Green
val CashFlowSecondary = Color(0xFF1976D2)      // Blue
val CashFlowBackground = Color(0xFF121212)     // Dark Background
val CashFlowSurface = Color(0xFF1E1E1E)        // Card Background
val CashFlowAccent = Color(0xFF4CAF50)         // Light Green Accent

val LightColorScheme = lightColorScheme(
    primary = CashFlowPrimary,
    onPrimary = Color.White,
    primaryContainer = CashFlowAccent,
    onPrimaryContainer = Color.White,
    secondary = CashFlowSecondary,
    onSecondary = Color.White,
    secondaryContainer = CashFlowSecondary.copy(alpha = 0.1f),
    onSecondaryContainer = CashFlowSecondary,
    tertiary = CashFlowAccent,
    onTertiary = Color.White,
    tertiaryContainer = CashFlowAccent.copy(alpha = 0.1f),
    onTertiaryContainer = CashFlowAccent,
    error = Color(0xFFFF5722),
    onError = Color.White,
    errorContainer = Color(0xFFFFEBEE),
    onErrorContainer = Color(0xFFD32F2F),
    surface = Color.White,
    surfaceDim = Color(0xFFF5F5F5),
    surfaceBright = Color.White,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFAFAFA),
    surfaceContainer = Color(0xFFF5F5F5),
    surfaceContainerHigh = Color(0xFFEEEEEE),
    surfaceContainerHighest = Color(0xFFE0E0E0),
    onSurface = Color(0xFF212121),
    onSurfaceVariant = Color(0xFF757575),
    outline = Color(0xFFBDBDBD),
    outlineVariant = Color(0xFFE0E0E0),
    inverseSurface = CashFlowBackground,
    inversePrimary = CashFlowPrimary,
    surfaceTint = CashFlowPrimary,
    scrim = Color(0x99000000),
)

val DarkColorScheme = darkColorScheme(
    primary = CashFlowPrimary,
    onPrimary = Color.Black,
    primaryContainer = CashFlowPrimaryDark,
    onPrimaryContainer = Color.White,
    secondary = CashFlowSecondary,
    onSecondary = Color.White,
    secondaryContainer = CashFlowSecondary.copy(alpha = 0.2f),
    onSecondaryContainer = CashFlowSecondary,
    tertiary = CashFlowAccent,
    onTertiary = Color.Black,
    tertiaryContainer = CashFlowAccent.copy(alpha = 0.2f),
    onTertiaryContainer = CashFlowAccent,
    error = Color(0xFFFF5722),
    onError = Color.White,
    errorContainer = Color(0xFF5D1A1A),
    onErrorContainer = Color(0xFFFFCDD2),
    surface = CashFlowSurface,
    surfaceDim = Color(0xFF0F0F0F),
    surfaceBright = Color(0xFF2C2C2C),
    surfaceContainerLowest = Color(0xFF0A0A0A),
    surfaceContainerLow = CashFlowBackground,
    surfaceContainer = CashFlowSurface,
    surfaceContainerHigh = Color(0xFF2C2C2C),
    surfaceContainerHighest = Color(0xFF383838),
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFBDBDBD),
    outline = Color(0xFF757575),
    outlineVariant = Color(0xFF424242),
    inverseSurface = Color.White,
    inversePrimary = CashFlowPrimaryDark,
    surfaceTint = CashFlowPrimary,
    scrim = Color(0x99000000),
)