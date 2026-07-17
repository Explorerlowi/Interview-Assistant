package com.example.interviewassistant.core.i18n

/**
 * Desktop strings for the currently selected operating-system language.
 */
class DesktopStringsProvider : StringsProvider {
    override fun get(id: AppStringId): String {
        val text = when (id) {
            AppStringId.APP_TITLE -> Text("Interview Assistant", "面试助手")
            AppStringId.COMMON_OK -> Text("OK", "确定")
            AppStringId.COMMON_CANCEL -> Text("Cancel", "取消")
            AppStringId.COMMON_SAVE -> Text("Save", "保存")
            AppStringId.COMMON_DELETE -> Text("Delete", "删除")
            AppStringId.COMMON_RETRY -> Text("Retry", "重试")
            AppStringId.COMMON_BACK -> Text("Back", "返回")
            AppStringId.LOGIN_TITLE -> Text("Login", "登录")
            AppStringId.LOGIN_USERNAME -> Text("Username", "用户名")
            AppStringId.LOGIN_BTN -> Text("Sign in", "登录")
            AppStringId.NAV_ASSISTANT -> Text("Assistant", "面试助手")
            AppStringId.NAV_MOCK_INTERVIEW -> Text("Mock interview", "模拟面试")
            AppStringId.NAV_SETTINGS -> Text("Settings", "设置")
            AppStringId.ASSISTANT_TITLE -> Text("Interview assistant", "面试助手")
            AppStringId.RESUME_LIBRARY_TITLE -> Text("Resume library", "简历库")
            AppStringId.RECENT_SESSIONS_TITLE -> Text("Recent sessions", "最近会话")
            AppStringId.IMPORT_RESUME -> Text("Import resume", "导入简历")
            AppStringId.IMPORT_RESUME_COMPLETED -> Text("Resume imported", "简历已导入")
            AppStringId.IMPORT_RESUME_HINT -> Text(
                "Click to choose, or drag / paste a PDF, JPG, or PNG",
                "点击选择，或拖入 / 粘贴 PDF、JPG、PNG",
            )
            AppStringId.IMPORT_RESUME_DROP_ACTIVE -> Text("Release to import", "松手即可导入")
            AppStringId.IMPORT_RESUME_PASTE -> Text("Paste", "粘贴")
            AppStringId.IMPORT_RESUME_UNSUPPORTED -> Text(
                "Only PDF, JPG, and PNG resumes are supported",
                "仅支持 PDF、JPG、PNG 简历",
            )
            AppStringId.IMPORT_RESUME_CLIPBOARD_EMPTY -> Text(
                "Clipboard has no supported resume file or image",
                "剪贴板中没有可用的简历文件或图片",
            )
            AppStringId.RESUME_EMPTY -> Text(
                "Import a PDF, JPG, or PNG resume to begin. You can also drag or paste one here.",
                "请导入 PDF、JPG 或 PNG 简历开始使用，也可拖入或粘贴。",
            )
            AppStringId.SESSION_EMPTY -> Text("No interview sessions yet.", "暂无面试会话。")
            AppStringId.SELECT_RESUME -> Text("Select resume", "选择简历")
            AppStringId.START_SESSION -> Text("Start session", "开始会话")
            AppStringId.OPEN_SESSION -> Text("Open", "打开")
            AppStringId.OCR_QUEUED -> Text("Waiting for OCR", "等待 OCR")
            AppStringId.OCR_PENDING -> Text("OCR pending", "OCR 排队中")
            AppStringId.OCR_RUNNING -> Text("OCR processing", "OCR 解析中")
            AppStringId.OCR_READY -> Text("Ready", "已就绪")
            AppStringId.OCR_FAILED -> Text("OCR failed", "OCR 失败")
            AppStringId.RESUME_VIEW_CONTENT -> Text("View content", "查看内容")
            AppStringId.RESUME_CONTENT_TITLE -> Text("Resume content", "简历内容")
            AppStringId.RESUME_RENDER_HTML -> Text("Render", "渲染显示")
            AppStringId.RESUME_SHOW_SOURCE -> Text("Source", "查看源码")
            AppStringId.RESUME_COPY -> Text("Copy", "复制")
            AppStringId.RESUME_COPIED -> Text("Copied", "已复制")
            AppStringId.RESUME_EDIT -> Text("Edit", "编辑")
            AppStringId.RESUME_SAVED -> Text("Resume text saved", "简历文本已保存")
            AppStringId.RESUME_VIEW_ORIGINAL -> Text("Original OCR", "OCR 原文")
            AppStringId.RESUME_VIEW_CURRENT -> Text("Current text", "当前文本")
            AppStringId.RESUME_RESTORE_ORIGINAL -> Text("Restore original", "恢复原文")
            AppStringId.RESUME_REDACT_PREVIEW -> Text("Privacy preview", "脱敏预览")
            AppStringId.RESUME_REDACT_ORIGINAL -> Text("Show original", "显示原文")
            AppStringId.OCR_PROCESSING_HINT -> Text("Parsing resume…", "正在解析简历…")
            AppStringId.WORKSPACE_TRANSCRIPT -> Text("Live transcript", "实时转写")
            AppStringId.WORKSPACE_ANSWER -> Text("Answer suggestion", "回答建议")
            AppStringId.START_LISTENING -> Text("Start listening", "开始听写")
            AppStringId.STOP_LISTENING -> Text("Stop listening", "停止听写")
            AppStringId.GENERATE_ANSWER -> Text("Generate answer", "生成回答")
            AppStringId.CANCEL_GENERATION -> Text("Cancel generation", "停止生成")
            AppStringId.COMPLETE_SESSION -> Text("Complete session", "结束会话")
            AppStringId.TRIGGER_MANUAL -> Text("Manual", "手动")
            AppStringId.TRIGGER_AUTOMATIC -> Text("Automatic", "自动")
            AppStringId.TRANSCRIPT_EMPTY -> Text(
                "Recognized speech appears here.",
                "识别到的面试问题将显示在这里。",
            )
            AppStringId.ANSWER_EMPTY -> Text(
                "Answer suggestions stream here.",
                "大模型回答建议将在这里流式显示。",
            )
            AppStringId.SETTINGS_TITLE -> Text("Provider settings", "服务配置")
            AppStringId.SETTINGS_SPEECH_RECOGNITION -> Text("Speech recognition", "语音识别")
            AppStringId.SETTINGS_SPEECH_XUNFEI -> Text("iFlytek cloud", "讯飞云端")
            AppStringId.SETTINGS_SPEECH_SENSEVOICE -> Text("SenseVoice on device", "SenseVoice 本地")
            AppStringId.SETTINGS_XUNFEI_DESCRIPTION -> Text(
                "Audio is streamed to iFlytek for cloud recognition.",
                "音频会发送到讯飞云端进行实时语音识别。",
            )
            AppStringId.SETTINGS_SENSEVOICE_DESCRIPTION -> Text(
                "Audio stays on this PC. Interviews use system audio; settings tests use the microphone.",
                "音频仅在本机处理。正式面试识别系统声音，设置测试使用麦克风。",
            )
            AppStringId.SETTINGS_SENSEVOICE_LANGUAGE -> Text("Recognition language", "识别语言")
            AppStringId.LANGUAGE_AUTO -> Text("Auto", "自动识别")
            AppStringId.LANGUAGE_CHINESE -> Text("Chinese", "中文")
            AppStringId.LANGUAGE_ENGLISH -> Text("English", "英文")
            AppStringId.LANGUAGE_CANTONESE -> Text("Cantonese", "粤语")
            AppStringId.LANGUAGE_JAPANESE -> Text("Japanese", "日语")
            AppStringId.LANGUAGE_KOREAN -> Text("Korean", "韩语")
            AppStringId.SETTINGS_SENSEVOICE_MODEL -> Text("Local model · about 231 MB", "本地模型 · 约 231 MB")
            AppStringId.SETTINGS_SENSEVOICE_MODEL_UNAVAILABLE -> Text(
                "Not supported on this platform",
                "当前平台不支持",
            )
            AppStringId.SETTINGS_SENSEVOICE_MODEL_NOT_INSTALLED -> Text("Not downloaded", "尚未下载")
            AppStringId.SETTINGS_SENSEVOICE_MODEL_PREPARING -> Text(
                "Connecting to a model source…",
                "正在连接模型下载源…",
            )
            AppStringId.SETTINGS_SENSEVOICE_MODEL_DOWNLOADING -> Text("Downloading", "正在下载")
            AppStringId.SETTINGS_SENSEVOICE_MODEL_READY -> Text("Ready", "已就绪")
            AppStringId.SETTINGS_SENSEVOICE_MODEL_FAILED -> Text(
                "Download or verification failed",
                "下载或校验失败",
            )
            AppStringId.SETTINGS_SENSEVOICE_FAILURE_NETWORK -> Text(
                "Unable to reach any model source. Check the network and retry.",
                "无法连接任何模型下载源，请检查网络后重试。",
            )
            AppStringId.SETTINGS_SENSEVOICE_FAILURE_TIMEOUT -> Text(
                "The model source timed out. Switch networks or retry.",
                "连接模型下载源超时，请切换网络或重试。",
            )
            AppStringId.SETTINGS_SENSEVOICE_FAILURE_SERVER -> Text(
                "The model source returned an error. Please retry later.",
                "模型下载源返回错误，请稍后重试。",
            )
            AppStringId.SETTINGS_SENSEVOICE_FAILURE_STORAGE -> Text(
                "Not enough storage, or the model file could not be written.",
                "存储空间不足，或无法写入模型文件。",
            )
            AppStringId.SETTINGS_SENSEVOICE_FAILURE_VERIFICATION -> Text(
                "The downloaded file failed integrity verification.",
                "下载文件未通过完整性校验。",
            )
            AppStringId.SETTINGS_SENSEVOICE_FAILURE_UNKNOWN -> Text(
                "Model installation failed unexpectedly.",
                "模型安装遇到未知错误。",
            )
            AppStringId.SETTINGS_SENSEVOICE_RETRY_HINT -> Text(
                "Downloaded progress is retained and will resume when you retry.",
                "已下载的进度会保留，重试时将继续下载。",
            )
            AppStringId.SETTINGS_SENSEVOICE_DOWNLOAD -> Text("Download model", "下载模型")
            AppStringId.SETTINGS_SENSEVOICE_TEST_TITLE -> Text("Speech recognition test", "语音识别测试")
            AppStringId.SETTINGS_SENSEVOICE_TEST_DESCRIPTION -> Text(
                "Tests the selected recognition service. Speak after starting and pause briefly after each sentence.",
                "测试当前选中的语音识别方式。开始后对着麦克风说话，每句话说完稍作停顿。",
            )
            AppStringId.SETTINGS_SENSEVOICE_TEST_CURRENT_MODEL -> Text(
                "Current test service / model",
                "当前测试服务 / 模型",
            )
            AppStringId.SETTINGS_SPEECH_TEST_XUNFEI_ENGINE -> Text(
                "iFlytek streaming dictation",
                "讯飞流式听写",
            )
            AppStringId.SETTINGS_SPEECH_TEST_CREDENTIALS_REQUIRED -> Text(
                "Save a complete iFlytek App ID and API credentials to enable testing.",
                "请先保存完整的讯飞 App ID 和 API 凭据再测试。",
            )
            AppStringId.SETTINGS_SPEECH_TEST_SAVED_CONFIG_HINT -> Text(
                "The iFlytek test uses saved settings and credentials. Save changes before testing.",
                "讯飞测试使用已保存的配置和凭据，修改后请先保存。",
            )
            AppStringId.SETTINGS_SENSEVOICE_TEST_MODEL_REQUIRED -> Text(
                "Download the local model to enable testing.",
                "下载本地模型后即可测试。",
            )
            AppStringId.SETTINGS_SENSEVOICE_TEST_IDLE -> Text("Ready to test", "可以开始测试")
            AppStringId.SETTINGS_SENSEVOICE_TEST_LISTENING -> Text(
                "Listening and recognizing…",
                "正在聆听并识别…",
            )
            AppStringId.SETTINGS_SENSEVOICE_TEST_RESULT_EMPTY -> Text(
                "Recognition results appear here.",
                "识别结果会显示在这里。",
            )
            AppStringId.SETTINGS_SENSEVOICE_TEST_START -> Text("Start test", "开始测试")
            AppStringId.SETTINGS_SENSEVOICE_TEST_STOP -> Text("Stop test", "停止测试")
            AppStringId.SETTINGS_SPEECH_UNDERSTANDING -> Text(
                "Speech understanding",
                "语音理解结果",
            )
            AppStringId.SETTINGS_DETECTED_LANGUAGE -> Text("Language", "语言")
            AppStringId.SETTINGS_DETECTED_EMOTION -> Text("Emotion", "情感")
            AppStringId.SETTINGS_DETECTED_AUDIO_EVENT -> Text("Audio event", "声音事件")
            AppStringId.SETTINGS_RESULT_PENDING -> Text("Waiting for speech", "等待识别")
            AppStringId.EMOTION_HAPPY -> Text("Happy", "开心")
            AppStringId.EMOTION_SAD -> Text("Sad", "悲伤")
            AppStringId.EMOTION_ANGRY -> Text("Angry", "愤怒")
            AppStringId.EMOTION_NEUTRAL -> Text("Neutral", "中性")
            AppStringId.EMOTION_FEARFUL -> Text("Fearful", "恐惧")
            AppStringId.EMOTION_DISGUSTED -> Text("Disgusted", "厌恶")
            AppStringId.EMOTION_SURPRISED -> Text("Surprised", "惊讶")
            AppStringId.AUDIO_EVENT_SPEECH -> Text("Speech", "说话")
            AppStringId.AUDIO_EVENT_BGM -> Text("Background music", "背景音乐")
            AppStringId.AUDIO_EVENT_APPLAUSE -> Text("Applause", "掌声")
            AppStringId.AUDIO_EVENT_LAUGHTER -> Text("Laughter", "笑声")
            AppStringId.AUDIO_EVENT_CRY -> Text("Crying", "哭声")
            AppStringId.AUDIO_EVENT_SNEEZE -> Text("Sneezing", "喷嚏")
            AppStringId.AUDIO_EVENT_BREATH -> Text("Breathing", "呼吸声")
            AppStringId.AUDIO_EVENT_COUGH -> Text("Coughing", "咳嗽")
            AppStringId.SETTINGS_PADDLE -> Text("PaddleOCR", "PaddleOCR")
            AppStringId.SETTINGS_XUNFEI -> Text("iFlytek dictation", "讯飞流式听写")
            AppStringId.SETTINGS_LLM -> Text("OpenAI-compatible model", "OpenAI 兼容大模型")
            AppStringId.SETTINGS_ENDPOINT -> Text("Endpoint", "接口地址")
            AppStringId.SETTINGS_MODEL -> Text("Model", "模型")
            AppStringId.SETTINGS_API_KEY -> Text("API key", "API Key")
            AppStringId.SETTINGS_API_SECRET -> Text("API secret", "API Secret")
            AppStringId.SETTINGS_APP_ID -> Text("App ID", "App ID")
            AppStringId.SETTINGS_SYSTEM_PROMPT -> Text("System prompt", "系统提示词")
            AppStringId.SETTINGS_THINKING -> Text("Thinking mode", "思考模式")
            AppStringId.SETTINGS_REDACT_PII -> Text(
                "Hide personal info sent to the model",
                "发送给模型前隐藏个人信息",
            )
            AppStringId.SETTINGS_CONFIGURED -> Text("Configured", "已配置")
            AppStringId.SETTINGS_NOT_CONFIGURED -> Text("Not configured", "未配置")
            AppStringId.SETTINGS_SHOW_SECRET -> Text("Show secret", "显示密钥")
            AppStringId.SETTINGS_HIDE_SECRET -> Text("Hide secret", "隐藏密钥")
            AppStringId.SETTINGS_SAVED -> Text("Settings saved", "设置已保存")
            AppStringId.SETTINGS_CLEAR_SECRETS -> Text("Clear credentials", "清除凭据")
            AppStringId.SETTINGS_TEST_CONNECTION -> Text("Test connection", "测试连接")
            AppStringId.MOCK_PLACEHOLDER_TITLE -> Text(
                "Mock interview is coming soon",
                "模拟面试即将上线",
            )
            AppStringId.MOCK_PLACEHOLDER_DESCRIPTION -> Text(
                "This module is reserved for a future guided interview experience.",
                "此模块已预留，后续将提供引导式模拟面试体验。",
            )
            AppStringId.DESKTOP_COMPACT_MODE -> Text("Compact mode", "紧凑模式")
            AppStringId.DESKTOP_ALWAYS_ON_TOP -> Text("Always on top", "窗口置顶")
            AppStringId.ERROR_PERMISSION_MICROPHONE -> Text(
                "Microphone permission is required.",
                "需要麦克风权限才能开始听写。",
            )
            AppStringId.ERROR_PROVIDER_NOT_CONFIGURED -> Text(
                "Complete provider settings first.",
                "请先完成服务配置。",
            )
            AppStringId.ERROR_SENSEVOICE_UNAVAILABLE -> Text(
                "On-device SenseVoice is unavailable on this platform.",
                "当前平台不支持本地 SenseVoice。",
            )
            AppStringId.ERROR_SENSEVOICE_MODEL_NOT_READY -> Text(
                "Download the SenseVoice model in settings first.",
                "请先在设置中下载 SenseVoice 模型。",
            )
            AppStringId.ERROR_SENSEVOICE_INFERENCE -> Text(
                "On-device speech recognition failed. Please retry.",
                "本地语音识别失败，请重试。",
            )
            AppStringId.ERROR_SPEECH_RECOGNITION -> Text(
                "Speech recognition failed. Please retry.",
                "语音识别失败，请重试。",
            )
            AppStringId.ERROR_GENERIC -> Text("Something went wrong. Please retry.", "发生错误，请重试。")
        }
        return if (getCurrentLanguage() == AppLanguage.ZH_CN) text.zh else text.en
    }

    private data class Text(val en: String, val zh: String)
}
