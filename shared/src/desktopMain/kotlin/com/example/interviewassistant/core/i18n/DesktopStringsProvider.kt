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
            AppStringId.ERROR_GENERIC -> Text("Something went wrong. Please retry.", "发生错误，请重试。")
        }
        return if (getCurrentLanguage() == AppLanguage.ZH_CN) text.zh else text.en
    }

    private data class Text(val en: String, val zh: String)
}
