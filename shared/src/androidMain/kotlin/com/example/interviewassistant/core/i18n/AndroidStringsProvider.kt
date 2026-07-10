package com.example.interviewassistant.core.i18n

import android.content.Context
import com.example.interviewassistant.shared.R

/**
 * Android [StringsProvider] backed by shared module string resources.
 */
class AndroidStringsProvider(private val context: Context) : StringsProvider {
    override fun get(id: AppStringId): String {
        val resId = when (id) {
            AppStringId.APP_TITLE -> R.string.app_title
            AppStringId.COMMON_OK -> R.string.common_ok
            AppStringId.COMMON_CANCEL -> R.string.common_cancel
            AppStringId.COMMON_SAVE -> R.string.common_save
            AppStringId.COMMON_DELETE -> R.string.common_delete
            AppStringId.COMMON_RETRY -> R.string.common_retry
            AppStringId.COMMON_BACK -> R.string.common_back
            AppStringId.LOGIN_TITLE -> R.string.login_title
            AppStringId.LOGIN_USERNAME -> R.string.login_username
            AppStringId.LOGIN_BTN -> R.string.login_btn
            AppStringId.NAV_ASSISTANT -> R.string.nav_assistant
            AppStringId.NAV_MOCK_INTERVIEW -> R.string.nav_mock_interview
            AppStringId.NAV_SETTINGS -> R.string.nav_settings
            AppStringId.ASSISTANT_TITLE -> R.string.assistant_title
            AppStringId.RESUME_LIBRARY_TITLE -> R.string.resume_library_title
            AppStringId.RECENT_SESSIONS_TITLE -> R.string.recent_sessions_title
            AppStringId.IMPORT_RESUME -> R.string.import_resume
            AppStringId.RESUME_EMPTY -> R.string.resume_empty
            AppStringId.SESSION_EMPTY -> R.string.session_empty
            AppStringId.SELECT_RESUME -> R.string.select_resume
            AppStringId.START_SESSION -> R.string.start_session
            AppStringId.OPEN_SESSION -> R.string.open_session
            AppStringId.OCR_QUEUED -> R.string.ocr_queued
            AppStringId.OCR_PENDING -> R.string.ocr_pending
            AppStringId.OCR_RUNNING -> R.string.ocr_running
            AppStringId.OCR_READY -> R.string.ocr_ready
            AppStringId.OCR_FAILED -> R.string.ocr_failed
            AppStringId.RESUME_VIEW_CONTENT -> R.string.resume_view_content
            AppStringId.RESUME_CONTENT_TITLE -> R.string.resume_content_title
            AppStringId.RESUME_RENDER_HTML -> R.string.resume_render_html
            AppStringId.RESUME_SHOW_SOURCE -> R.string.resume_show_source
            AppStringId.RESUME_COPY -> R.string.resume_copy
            AppStringId.RESUME_COPIED -> R.string.resume_copied
            AppStringId.OCR_PROCESSING_HINT -> R.string.ocr_processing_hint
            AppStringId.WORKSPACE_TRANSCRIPT -> R.string.workspace_transcript
            AppStringId.WORKSPACE_ANSWER -> R.string.workspace_answer
            AppStringId.START_LISTENING -> R.string.start_listening
            AppStringId.STOP_LISTENING -> R.string.stop_listening
            AppStringId.GENERATE_ANSWER -> R.string.generate_answer
            AppStringId.CANCEL_GENERATION -> R.string.cancel_generation
            AppStringId.COMPLETE_SESSION -> R.string.complete_session
            AppStringId.TRIGGER_MANUAL -> R.string.trigger_manual
            AppStringId.TRIGGER_AUTOMATIC -> R.string.trigger_automatic
            AppStringId.TRANSCRIPT_EMPTY -> R.string.transcript_empty
            AppStringId.ANSWER_EMPTY -> R.string.answer_empty
            AppStringId.SETTINGS_TITLE -> R.string.settings_title
            AppStringId.SETTINGS_PADDLE -> R.string.settings_paddle
            AppStringId.SETTINGS_XUNFEI -> R.string.settings_xunfei
            AppStringId.SETTINGS_LLM -> R.string.settings_llm
            AppStringId.SETTINGS_ENDPOINT -> R.string.settings_endpoint
            AppStringId.SETTINGS_MODEL -> R.string.settings_model
            AppStringId.SETTINGS_API_KEY -> R.string.settings_api_key
            AppStringId.SETTINGS_API_SECRET -> R.string.settings_api_secret
            AppStringId.SETTINGS_APP_ID -> R.string.settings_app_id
            AppStringId.SETTINGS_SYSTEM_PROMPT -> R.string.settings_system_prompt
            AppStringId.SETTINGS_THINKING -> R.string.settings_thinking
            AppStringId.SETTINGS_CONFIGURED -> R.string.settings_configured
            AppStringId.SETTINGS_NOT_CONFIGURED -> R.string.settings_not_configured
            AppStringId.SETTINGS_SHOW_SECRET -> R.string.settings_show_secret
            AppStringId.SETTINGS_HIDE_SECRET -> R.string.settings_hide_secret
            AppStringId.SETTINGS_SAVED -> R.string.settings_saved
            AppStringId.SETTINGS_CLEAR_SECRETS -> R.string.settings_clear_secrets
            AppStringId.SETTINGS_TEST_CONNECTION -> R.string.settings_test_connection
            AppStringId.MOCK_PLACEHOLDER_TITLE -> R.string.mock_placeholder_title
            AppStringId.MOCK_PLACEHOLDER_DESCRIPTION -> R.string.mock_placeholder_description
            AppStringId.DESKTOP_COMPACT_MODE -> R.string.desktop_compact_mode
            AppStringId.DESKTOP_ALWAYS_ON_TOP -> R.string.desktop_always_on_top
            AppStringId.ERROR_PERMISSION_MICROPHONE -> R.string.error_permission_microphone
            AppStringId.ERROR_PROVIDER_NOT_CONFIGURED -> R.string.error_provider_not_configured
            AppStringId.ERROR_GENERIC -> R.string.error_generic
        }
        return context.getString(resId)
    }
}
