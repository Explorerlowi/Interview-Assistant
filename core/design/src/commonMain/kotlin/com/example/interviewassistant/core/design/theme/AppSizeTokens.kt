package com.example.interviewassistant.core.design.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Spacing scale used by screens and reusable components.
 */
@Immutable
data class AppSpacingTokens(
    val xxs: Dp = 2.dp,
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 20.dp,
    val xxl: Dp = 24.dp,
    val screenHorizontal: Dp = 16.dp,
)

/**
 * Corner radius values for surfaces and controls.
 */
@Immutable
data class AppRadiusTokens(
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 10.dp,
    val lg: Dp = 14.dp,
    val xl: Dp = 18.dp,
)

/**
 * Fixed heights for common controls.
 */
@Immutable
data class AppComponentSizeTokens(
    val topBarHeight: Dp = 52.dp,
    val buttonMediumHeight: Dp = 44.dp,
    val buttonLargeHeight: Dp = 48.dp,
    val inputHeight: Dp = 46.dp,
    val listItemMinHeight: Dp = 52.dp,
    val iconButtonSize: Dp = 40.dp,
)
