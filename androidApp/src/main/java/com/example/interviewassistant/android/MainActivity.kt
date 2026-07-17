package com.example.interviewassistant.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.interviewassistant.android.feature.main.MainShellScreen
import com.example.interviewassistant.android.ui.theme.MyAppTheme
import com.example.interviewassistant.core.design.theme.AppDesign

/** Hosts the Android Compose application and applies edge-to-edge system bars. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = AppDesign.colors.pageBackground,
                ) {
                    MainShellScreen()
                }
            }
        }
    }
}
