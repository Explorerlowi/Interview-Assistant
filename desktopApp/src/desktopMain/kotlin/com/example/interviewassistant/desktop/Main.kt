package com.example.interviewassistant.desktop

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.example.interviewassistant.core.design.theme.AppDesignTheme
import com.example.interviewassistant.desktop.feature.login.DesktopLoginScreen
import com.example.interviewassistant.di.initKoin

/**
 * Desktop application entry point.
 */
fun main() {
    initKoin()
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Interview Assistant",
            state = rememberWindowState(width = 420.dp, height = 640.dp),
        ) {
            AppDesignTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    DesktopLoginScreen()
                }
            }
        }
    }
}
