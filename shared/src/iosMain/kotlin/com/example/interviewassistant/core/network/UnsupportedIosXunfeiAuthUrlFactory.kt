package com.example.interviewassistant.core.network

/**
 * Placeholder signer while iOS is outside the first product scope.
 */
class UnsupportedIosXunfeiAuthUrlFactory : XunfeiAuthUrlFactory {
    override fun create(endpoint: String, apiKey: String, apiSecret: String): String {
        error("iFlytek authentication is not available on iOS yet")
    }
}
