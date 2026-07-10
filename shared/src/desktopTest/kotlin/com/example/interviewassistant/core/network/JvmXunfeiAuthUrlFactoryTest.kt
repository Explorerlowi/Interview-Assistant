package com.example.interviewassistant.core.network

import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JvmXunfeiAuthUrlFactoryTest {
    @Test
    fun `signed URL contains RFC1123 date host and encoded authorization`() {
        val date = "Fri, 10 Jul 2026 03:00:00 GMT"
        val factory = JvmXunfeiAuthUrlFactory { date }

        val signed = factory.create(
            endpoint = "wss://iat-api.xfyun.cn/v2/iat",
            apiKey = "api-key",
            apiSecret = "api-secret",
        )

        val query = URI(signed).rawQuery.split('&').associate { pair ->
            val (key, value) = pair.split('=', limit = 2)
            key to URLDecoder.decode(value, StandardCharsets.UTF_8)
        }
        assertEquals("iat-api.xfyun.cn", query["host"])
        assertEquals(date, query["date"])
        val authorization = Base64.getDecoder().decode(query.getValue("authorization")).decodeToString()
        assertTrue(authorization.contains("""api_key="api-key""""))
        assertTrue(authorization.contains("""headers="host date request-line""""))
        assertTrue(authorization.contains("signature="))
        assertFalse(signed.contains("api-secret"))
    }
}
