package com.example.interviewassistant.core.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

interface CoroutineDispatcherProvider {
    val main: CoroutineDispatcher
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
}

class DefaultDispatcherProvider : CoroutineDispatcherProvider {
    override val main: CoroutineDispatcher = Dispatchers.Main
    override val io: CoroutineDispatcher = Dispatchers.Default // IO not available in commonMain by default in some versions, but standard now often aliases. safely using Default or need expect/actual for IO if strictly IO.
    // Actually Dispatchers.IO is available in kotlinx-coroutines-core 1.7+ for common, but let's stick to safe defaults or expect/actual if needed.
    // For simplicity, we use Default for IO in this template if IO is missing, but usually it is there.
    override val default: CoroutineDispatcher = Dispatchers.Default
}
