package com.example.interviewassistant.core.util

/**
 * Cross-platform time provider.
 */
expect object TimeProvider {
    /**
     * @return Current epoch time in milliseconds.
     */
    fun currentTimeMillis(): Long
}
