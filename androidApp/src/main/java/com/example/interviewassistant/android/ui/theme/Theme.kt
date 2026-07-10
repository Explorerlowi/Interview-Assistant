package com.example.interviewassistant.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import com.example.interviewassistant.core.design.theme.AppDesignTheme
import com.example.interviewassistant.core.design.theme.AppDesignThemeStyle

/**
 * Android theme bridge into [AppDesignTheme].
 */
@Composable
fun MyAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    AppDesignTheme(
        style = if (darkTheme) AppDesignThemeStyle.Dark else AppDesignThemeStyle.Light,
        content = content,
    )
}
