package com.example.interviewassistant.core.design.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Semantic color tokens for the template design system.
 *
 * Keep a single Light/Dark pair in the template; product apps can extend palettes later.
 */
@Immutable
data class AppColorTokens(
    val pageBackground: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceMuted: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val divider: Color,
    val border: Color,
    val brand: Color,
    val brandPressed: Color,
    val brandSubtle: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val onBrand: Color,
)

/**
 * Visual style selected by the app shell.
 */
enum class AppDesignThemeStyle {
    Light,
    Dark,
}

internal val LightColors = AppColorTokens(
    pageBackground = Color(0xFFF6F7F9),
    surface = Color.White,
    surfaceElevated = Color.White,
    surfaceMuted = Color(0xFFF2F4F7),
    textPrimary = Color(0xFF111827),
    textSecondary = Color(0xFF5F6673),
    textTertiary = Color(0xFF9AA1AD),
    divider = Color(0xFFEDEFF3),
    border = Color(0xFFE1E5EB),
    brand = Color(0xFF2563EB),
    brandPressed = Color(0xFF1D4ED8),
    brandSubtle = Color(0xFFDBEAFE),
    success = Color(0xFF18A058),
    warning = Color(0xFFFFA940),
    danger = Color(0xFFFF4D4F),
    onBrand = Color.White,
)

internal val DarkColors = AppColorTokens(
    pageBackground = Color(0xFF111418),
    surface = Color(0xFF191D23),
    surfaceElevated = Color(0xFF20252C),
    surfaceMuted = Color(0xFF252B33),
    textPrimary = Color(0xFFF4F6F8),
    textSecondary = Color(0xFFB8C0CC),
    textTertiary = Color(0xFF7F8896),
    divider = Color(0xFF2D333D),
    border = Color(0xFF363D48),
    brand = Color(0xFF60A5FA),
    brandPressed = Color(0xFF3B82F6),
    brandSubtle = Color(0xFF1E3A5F),
    success = Color(0xFF52C41A),
    warning = Color(0xFFFFC069),
    danger = Color(0xFFFF7875),
    onBrand = Color(0xFF0B1220),
)
