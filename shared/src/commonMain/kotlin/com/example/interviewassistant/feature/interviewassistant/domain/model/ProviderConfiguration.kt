package com.example.interviewassistant.feature.interviewassistant.domain.model

/**
 * Controls when a finalized transcript is submitted to the language model.
 */
enum class AnswerTriggerMode {
    MANUAL,
    AUTOMATIC,
}

/**
 * Selects the speech-recognition backend used by live interview sessions.
 */
enum class SpeechRecognitionMode {
    XUNFEI,
    SENSE_VOICE_ON_DEVICE,
}

/**
 * Additional understanding attributes returned with a speech-recognition segment.
 *
 * Values are normalized runtime labels. Unknown future labels are preserved instead of discarded.
 *
 * @property language Detected or selected language code.
 * @property emotion Detected speech-emotion label.
 * @property audioEvent Detected acoustic-event label.
 */
data class SpeechUnderstandingMetadata(
    val language: String? = null,
    val emotion: String? = null,
    val audioEvent: String? = null,
) {
    /** Whether no understanding attribute was returned by the recognizer. */
    val isEmpty: Boolean
        get() = language == null && emotion == null && audioEvent == null
}

/**
 * Non-secret settings for the on-device SenseVoice recognizer.
 *
 * @property language SenseVoice language code. Supported values are `zh`, `en`, `ja`, `ko`, `yue`, and `auto`.
 * @property useInverseTextNormalization Whether numbers and punctuation are normalized for display.
 * @property partialIntervalMillis Minimum interval between simulated-streaming partial decodes.
 * @property maxSpeechDurationSeconds Maximum VAD segment duration before it is finalized.
 */
data class SenseVoiceConfiguration(
    val language: String = "zh",
    val useInverseTextNormalization: Boolean = true,
    val partialIntervalMillis: Int = 800,
    val maxSpeechDurationSeconds: Int = 15,
)

/**
 * Non-secret PaddleOCR connection settings.
 *
 * @property endpoint Asynchronous OCR job endpoint.
 * @property model PaddleOCR model identifier.
 */
data class PaddleConfiguration(
    val endpoint: String = DEFAULT_ENDPOINT,
    val model: String = DEFAULT_MODEL,
) {
    companion object {
        const val DEFAULT_ENDPOINT = "https://paddleocr.aistudio-app.com/api/v2/ocr/jobs"
        const val DEFAULT_MODEL = "PaddleOCR-VL-1.6"
    }
}

/**
 * Non-secret iFlytek streaming dictation settings.
 *
 * @property endpoint Secure WebSocket endpoint.
 * @property language Recognition language.
 * @property domain Recognition domain.
 * @property accent Recognition accent.
 * @property endOfSpeechMillis Silence duration used for endpoint detection.
 */
data class XunfeiConfiguration(
    val endpoint: String = DEFAULT_ENDPOINT,
    val language: String = "zh_cn",
    val domain: String = "iat",
    val accent: String = "mandarin",
    val endOfSpeechMillis: Int = 2_000,
) {
    companion object {
        const val DEFAULT_ENDPOINT = "wss://iat-api.xfyun.cn/v2/iat"
    }
}

/**
 * OpenAI-compatible language-model settings.
 *
 * @property baseUrl Provider API root URL.
 * @property model Model identifier sent in chat-completion requests.
 * @property systemPrompt System message used when generating interview answers.
 * @property thinkingEnabled Enables DeepSeek thinking mode for the DeepSeek preset.
 * @property maxContextCharacters Character budget used when composing interview context.
 * @property redactPersonalInfo Replaces personal info (phone, email, ID, etc.) before sending text to the model.
 */
data class LlmConfiguration(
    val baseUrl: String = DEFAULT_BASE_URL,
    val model: String = DEFAULT_MODEL,
    val systemPrompt: String = DEFAULT_SYSTEM_PROMPT,
    val thinkingEnabled: Boolean = false,
    val maxContextCharacters: Int = 24_000,
    val redactPersonalInfo: Boolean = true,
) {
    companion object {
        const val DEFAULT_BASE_URL = "https://api.deepseek.com"
        const val DEFAULT_MODEL = "deepseek-v4-flash"
        const val DEFAULT_SYSTEM_PROMPT =
            "你是实时面试回答助手。必须基于候选人简历与问题给出真实、简洁、可直接口述的建议；" +
                "不得编造简历中不存在的经历。先给出核心回答，再列出可选补充点和风险提醒。" +
                "回答语言跟随面试问题，避免暴露你是辅助工具。"
    }
}

/**
 * Complete non-secret application configuration.
 */
data class ProviderConfiguration(
    val paddle: PaddleConfiguration = PaddleConfiguration(),
    val xunfei: XunfeiConfiguration = XunfeiConfiguration(),
    val speechRecognitionMode: SpeechRecognitionMode = SpeechRecognitionMode.XUNFEI,
    val senseVoice: SenseVoiceConfiguration = SenseVoiceConfiguration(),
    val llm: LlmConfiguration = LlmConfiguration(),
    val answerTriggerMode: AnswerTriggerMode = AnswerTriggerMode.MANUAL,
)

/**
 * Credential updates entered by the user.
 *
 * A `null` value preserves the existing secret. An empty value removes it.
 */
data class ProviderSecretUpdate(
    val paddleToken: String? = null,
    val xunfeiAppId: String? = null,
    val xunfeiApiKey: String? = null,
    val xunfeiApiSecret: String? = null,
    val llmApiKey: String? = null,
)

/**
 * Indicates which credentials are configured without exposing their values to UI code.
 */
data class ProviderSecretStatus(
    val hasPaddleToken: Boolean,
    val hasXunfeiCredentials: Boolean,
    val hasLlmApiKey: Boolean,
)

/**
 * Credential values loaded into the settings form for editing.
 *
 * Empty strings mean the corresponding secret is not stored yet.
 */
data class ProviderSecrets(
    val paddleToken: String = "",
    val xunfeiAppId: String = "",
    val xunfeiApiKey: String = "",
    val xunfeiApiSecret: String = "",
    val llmApiKey: String = "",
)

/**
 * Runtime iFlytek credentials read only by the speech data source.
 */
data class XunfeiCredentials(
    val appId: String,
    val apiKey: String,
    val apiSecret: String,
)
