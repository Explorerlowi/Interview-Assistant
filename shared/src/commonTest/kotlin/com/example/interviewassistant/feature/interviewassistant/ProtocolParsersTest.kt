package com.example.interviewassistant.feature.interviewassistant

import com.example.interviewassistant.feature.interviewassistant.data.remote.llm.parseOpenAiSse
import com.example.interviewassistant.feature.interviewassistant.data.remote.ocr.OcrDisplayHtml
import com.example.interviewassistant.feature.interviewassistant.data.remote.ocr.parseJsonLines
import com.example.interviewassistant.feature.interviewassistant.data.remote.ocr.parseOcrDocument
import com.example.interviewassistant.feature.interviewassistant.data.remote.speech.XunfeiCandidateWord
import com.example.interviewassistant.feature.interviewassistant.data.remote.speech.XunfeiResult
import com.example.interviewassistant.feature.interviewassistant.data.remote.speech.XunfeiTranscriptReducer
import com.example.interviewassistant.feature.interviewassistant.data.remote.speech.XunfeiWordSegment
import com.example.interviewassistant.feature.interviewassistant.data.remote.speech.XunfeiSessionPolicy
import com.example.interviewassistant.feature.interviewassistant.domain.model.LlmConfiguration
import com.example.interviewassistant.feature.interviewassistant.domain.usecase.InterviewAnswerContext
import com.example.interviewassistant.feature.interviewassistant.domain.usecase.InterviewPromptBuilder
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProtocolParsersTest {
    @Test
    fun `PaddleOCR JSONL preserves page order and collects image urls`() {
        val jsonl = """
            {"result":{"layoutParsingResults":[{"markdown":{"text":"第一页","images":{"imgs/a.jpg":"https://cdn/a.jpg"}}}]}}
            {"result":{"layoutParsingResults":[{"markdown":{"text":"第二页","images":{"imgs/b.jpg":"https://cdn/b.jpg"}},"outputImages":{"x":"url"}}]}}
        """.trimIndent()

        val document = parseOcrDocument(jsonl)
        assertEquals("第一页\n\n第二页", document.text)
        assertEquals(
            mapOf(
                "imgs/a.jpg" to "https://cdn/a.jpg",
                "imgs/b.jpg" to "https://cdn/b.jpg",
            ),
            document.images,
        )
        assertEquals("第一页\n\n第二页", parseJsonLines(jsonl))
    }

    @Test
    fun `OpenAI SSE parser emits content reasoning and stops at done`() = runTest {
        val body = """
            : keep-alive
            data: {"choices":[{"delta":{"reasoning_content":"思考"}}]}

            data: {"choices":[{"delta":{"content":"回答"}}]}

            data: {"choices":[{"delta":{},"finish_reason":"stop"}]}

            data: [DONE]

            data: {"choices":[{"delta":{"content":"不应读取"}}]}

        """.trimIndent()
        val chunks = mutableListOf<String>()
        val reasoning = mutableListOf<String>()

        parseOpenAiSse(ByteReadChannel(body)) {
            chunks += it.content
            reasoning += it.reasoningContent
        }

        assertEquals("回答", chunks.joinToString(""))
        assertEquals("思考", reasoning.joinToString(""))
        assertFalse(chunks.contains("不应读取"))
    }

    @Test
    fun `iFlytek reducer applies append and replacement ranges`() {
        val reducer = XunfeiTranscriptReducer()

        assertEquals("你好", reducer.accept(result(0, "apd", null, "你好")))
        assertEquals("你好世界", reducer.accept(result(1, "apd", null, "世界")))
        assertEquals("您好", reducer.accept(result(1, "rpl", listOf(0, 1), "您好")))
    }

    @Test
    fun `iFlytek session rotates before provider hard limit`() {
        assertFalse(XunfeiSessionPolicy.shouldRotate(1_000, 55_999))
        assertTrue(XunfeiSessionPolicy.shouldRotate(1_000, 56_000))
    }

    @Test
    fun `prompt builder keeps current question and bounds oversized resume`() {
        val messages = InterviewPromptBuilder().build(
            context = InterviewAnswerContext(
                resumeText = "R".repeat(20_000),
                question = "请介绍最有挑战的项目",
                recentTranscript = "T".repeat(8_000),
                recentAnswers = listOf("A".repeat(8_000)),
            ),
            maxCharacters = 4_000,
        )

        assertEquals(2, messages.size)
        assertEquals(LlmConfiguration.DEFAULT_SYSTEM_PROMPT, messages.first().content)
        assertTrue(messages.last().content.contains("请介绍最有挑战的项目"))
        assertTrue(messages.last().content.length < 5_000)
    }

    @Test
    fun `prompt builder uses custom system prompt`() {
        val messages = InterviewPromptBuilder().build(
            context = InterviewAnswerContext(
                resumeText = "简历",
                question = "自我介绍",
            ),
            maxCharacters = 4_000,
            systemPrompt = "用一句话回答。",
        )

        assertEquals("用一句话回答。", messages.first().content)
    }

    @Test
    fun `OCR display html converts latex underline and markdown headers`() {
        val raw = """
            # 标题
            使用 $ \underline{\text{豆包企业版大模型算法（原“云雀大模型算法-企业版”）}} $ 合作
            以及 $ \underline{\text{豆包企业版大模型算法}} $ 说明
        """.trimIndent()

        val html = OcrDisplayHtml.normalize(raw)
        assertTrue(html.contains("<h1>标题</h1>"))
        assertTrue(html.contains("<u>豆包企业版大模型算法（原“云雀大模型算法-企业版”）</u>"))
        assertTrue(html.contains("<u>豆包企业版大模型算法</u>"))
        assertFalse(html.contains("\\underline"))
        assertFalse(html.contains("$"))
        assertEquals(
            "使用 豆包企业版大模型算法 合作",
            OcrDisplayHtml.plainPreview("使用 $ \\underline{\\text{豆包企业版大模型算法}} $ 合作"),
        )
    }

    @Test
    fun `OCR display html preserves plain text line breaks`() {
        val raw = """
            段落结束。
            1. 备案编号：网信算备110108823483901230065号；
            2. 备案系统截图：
            <div style="text-align: center;"><img src="imgs/a.jpg" alt="Image" /></div>
            3. 拟公示算法机制机理内容：
        """.trimIndent()

        val html = OcrDisplayHtml.normalize(raw)
        assertTrue(html.contains("段落结束。<br/>1. 备案编号"))
        assertTrue(html.contains("号；<br/>2. 备案系统截图："))
        assertTrue(html.contains("""<div style="text-align: center;"><img src="imgs/a.jpg" alt="Image" /></div>"""))
        assertTrue(html.contains("</div><br/>3. 拟公示") || html.contains("</div>3. 拟公示"))
        assertTrue(
            OcrDisplayHtml.normalize("使用。1. 备案编号：A；2. 截图：")
                .contains("使用。<br/>1. 备案编号：A；<br/>2. 截图："),
        )
    }

    private fun result(
        sequence: Int,
        pgs: String,
        range: List<Int>?,
        text: String,
    ): XunfeiResult {
        return XunfeiResult(
            sn = sequence,
            pgs = pgs,
            rg = range,
            ws = listOf(
                XunfeiWordSegment(
                    cw = listOf(XunfeiCandidateWord(text)),
                ),
            ),
        )
    }
}
