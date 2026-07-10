package com.example.interviewassistant.core.network

import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Android/JVM iFlytek handshake signer.
 */
class JvmXunfeiAuthUrlFactory(
    private val dateProvider: () -> String = {
        ZonedDateTime.now(java.time.ZoneOffset.UTC).format(DateTimeFormatter.RFC_1123_DATE_TIME)
    },
) : XunfeiAuthUrlFactory {
    override fun create(endpoint: String, apiKey: String, apiSecret: String): String {
        val uri = URI(endpoint)
        val host = requireNotNull(uri.host) { "iFlytek endpoint must include a host" }
        val path = uri.rawPath.ifBlank { "/" }
        val date = dateProvider()
        val signatureOrigin = "host: $host\ndate: $date\nGET $path HTTP/1.1"
        val mac = Mac.getInstance(HMAC_SHA_256)
        mac.init(SecretKeySpec(apiSecret.encodeToByteArray(), HMAC_SHA_256))
        val signature = Base64.getEncoder().encodeToString(mac.doFinal(signatureOrigin.encodeToByteArray()))
        val authorizationOrigin =
            """api_key="$apiKey", algorithm="hmac-sha256", headers="host date request-line", signature="$signature""""
        val authorization = Base64.getEncoder().encodeToString(authorizationOrigin.encodeToByteArray())
        val separator = if (endpoint.contains('?')) '&' else '?'
        return buildString {
            append(endpoint)
            append(separator)
            append("authorization=")
            append(urlEncode(authorization))
            append("&date=")
            append(urlEncode(date))
            append("&host=")
            append(urlEncode(host))
        }
    }

    private fun urlEncode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.name())

    private companion object {
        const val HMAC_SHA_256 = "HmacSHA256"
    }
}
