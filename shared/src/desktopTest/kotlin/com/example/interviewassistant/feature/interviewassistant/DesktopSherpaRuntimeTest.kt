package com.example.interviewassistant.feature.interviewassistant

import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.sun.jna.Platform
import kotlin.test.Test
import kotlin.test.assertFailsWith

class DesktopSherpaRuntimeTest {
    @Test
    fun windowsNativeLibraryLoadsBeforeInvalidConfigurationIsRejected() {
        if (!Platform.isWindows()) return

        val modelConfig = OfflineModelConfig.builder().build()
        val recognizerConfig = OfflineRecognizerConfig.builder()
            .setOfflineModelConfig(modelConfig)
            .build()

        assertFailsWith<IllegalArgumentException> {
            OfflineRecognizer(recognizerConfig)
        }
    }
}
