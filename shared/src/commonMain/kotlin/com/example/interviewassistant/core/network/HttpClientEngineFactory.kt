package com.example.interviewassistant.core.network

import io.ktor.client.engine.HttpClientEngine

/**
 * Creates a platform-specific Ktor [HttpClientEngine].
 */
expect fun createHttpClientEngine(): HttpClientEngine
