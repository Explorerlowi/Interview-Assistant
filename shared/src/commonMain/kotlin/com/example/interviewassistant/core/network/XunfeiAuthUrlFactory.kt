package com.example.interviewassistant.core.network

/**
 * Builds the HMAC-authenticated iFlytek WebSocket URL using platform crypto and UTC time APIs.
 */
interface XunfeiAuthUrlFactory {
    /** Returns a signed URL suitable for one WebSocket handshake. */
    fun create(endpoint: String, apiKey: String, apiSecret: String): String
}
