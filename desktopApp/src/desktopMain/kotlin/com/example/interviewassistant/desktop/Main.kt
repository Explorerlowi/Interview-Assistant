package com.example.interviewassistant.desktop

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.useResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.example.interviewassistant.core.design.theme.AppDesignTheme
import com.example.interviewassistant.core.i18n.AppStringId
import com.example.interviewassistant.core.i18n.StringsProvider
import com.example.interviewassistant.desktop.feature.main.DesktopMainShell
import com.example.interviewassistant.di.initKoin
import org.koin.core.context.GlobalContext

/**
 * Desktop application entry point.
 */
fun main() {
    initKoin()
    val strings = GlobalContext.get().get<StringsProvider>()
    application {
        var compactMode by remember { mutableStateOf(false) }
        var alwaysOnTop by remember { mutableStateOf(false) }
        val windowState = rememberWindowState(width = 1_180.dp, height = 760.dp)
        val appIcon = remember {
            BitmapPainter(useResource("icon.png", ::loadImageBitmap))
        }
        LaunchedEffect(compactMode) {
            windowState.size = if (compactMode) {
                DpSize(560.dp, 720.dp)
            } else {
                DpSize(1_180.dp, 760.dp)
            }
        }
        Window(
            onCloseRequest = ::exitApplication,
            title = strings.get(AppStringId.APP_TITLE),
            state = windowState,
            alwaysOnTop = alwaysOnTop,
            icon = appIcon,
        ) {
            AppDesignTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    DesktopMainShell(
                        strings = strings,
                        compactMode = compactMode,
                        alwaysOnTop = alwaysOnTop,
                        onCompactModeChange = { compactMode = it },
                        onAlwaysOnTopChange = { alwaysOnTop = it },
                    )
                }
            }
        }
    }
}
