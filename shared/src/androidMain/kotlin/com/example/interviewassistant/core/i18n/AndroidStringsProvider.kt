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
            AppStringId.IMPORT_RESUME_COMPLETED -> R.string.import_resume_completed
            AppStringId.IMPORT_RESUME_HINT -> R.string.import_resume_hint
            AppStringId.IMPORT_RESUME_DROP_ACTIVE -> R.string.import_resume_drop_active
            AppStringId.IMPORT_RESUME_PASTE -> R.string.import_resume_paste
            AppStringId.IMPORT_RESUME_UNSUPPORTED -> R.string.import_resume_unsupported
            AppStringId.IMPORT_RESUME_CLIPBOARD_EMPTY -> R.string.import_resume_clipboard_empty
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
            AppStringId.RESUME_EDIT -> R.string.resume_edit
            AppStringId.RESUME_SAVED -> R.string.resume_saved
            AppStringId.RESUME_VIEW_ORIGINAL -> R.string.resume_view_original
            AppStringId.RESUME_VIEW_CURRENT -> R.string.resume_view_current
            AppStringId.RESUME_RESTORE_ORIGINAL -> R.string.resume_restore_original
            AppStringId.RESUME_REDACT_PREVIEW -> R.string.resume_redact_preview
            AppStringId.RESUME_REDACT_ORIGINAL -> R.string.resume_redact_original
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
            AppStringId.SETTINGS_SPEECH_RECOGNITION -> R.string.settings_speech_recognition
            AppStringId.SETTINGS_SPEECH_XUNFEI -> R.string.settings_speech_xunfei
            AppStringId.SETTINGS_SPEECH_SENSEVOICE -> R.string.settings_speech_sensevoice
            AppStringId.SETTINGS_XUNFEI_DESCRIPTION -> R.string.settings_xunfei_description
            AppStringId.SETTINGS_SENSEVOICE_DESCRIPTION -> R.string.settings_sensevoice_description
            AppStringId.SETTINGS_SENSEVOICE_LANGUAGE -> R.string.settings_sensevoice_language
            AppStringId.LANGUAGE_AUTO -> R.string.language_auto
            AppStringId.LANGUAGE_CHINESE -> R.string.language_chinese
            AppStringId.LANGUAGE_ENGLISH -> R.string.language_english
            AppStringId.LANGUAGE_CANTONESE -> R.string.language_cantonese
            AppStringId.LANGUAGE_JAPANESE -> R.string.language_japanese
            AppStringId.LANGUAGE_KOREAN -> R.string.language_korean
            AppStringId.SETTINGS_SENSEVOICE_MODEL -> R.string.settings_sensevoice_model
            AppStringId.SETTINGS_SENSEVOICE_MODEL_UNAVAILABLE -> R.string.settings_sensevoice_model_unavailable
            AppStringId.SETTINGS_SENSEVOICE_MODEL_NOT_INSTALLED -> R.string.settings_sensevoice_model_not_installed
            AppStringId.SETTINGS_SENSEVOICE_MODEL_PREPARING -> R.string.settings_sensevoice_model_preparing
            AppStringId.SETTINGS_SENSEVOICE_MODEL_DOWNLOADING -> R.string.settings_sensevoice_model_downloading
            AppStringId.SETTINGS_SENSEVOICE_MODEL_READY -> R.string.settings_sensevoice_model_ready
            AppStringId.SETTINGS_SENSEVOICE_MODEL_FAILED -> R.string.settings_sensevoice_model_failed
            AppStringId.SETTINGS_SENSEVOICE_FAILURE_NETWORK -> R.string.settings_sensevoice_failure_network
            AppStringId.SETTINGS_SENSEVOICE_FAILURE_TIMEOUT -> R.string.settings_sensevoice_failure_timeout
            AppStringId.SETTINGS_SENSEVOICE_FAILURE_SERVER -> R.string.settings_sensevoice_failure_server
            AppStringId.SETTINGS_SENSEVOICE_FAILURE_STORAGE -> R.string.settings_sensevoice_failure_storage
            AppStringId.SETTINGS_SENSEVOICE_FAILURE_VERIFICATION -> R.string.settings_sensevoice_failure_verification
            AppStringId.SETTINGS_SENSEVOICE_FAILURE_UNKNOWN -> R.string.settings_sensevoice_failure_unknown
            AppStringId.SETTINGS_SENSEVOICE_RETRY_HINT -> R.string.settings_sensevoice_retry_hint
            AppStringId.SETTINGS_SENSEVOICE_DOWNLOAD -> R.string.settings_sensevoice_download
            AppStringId.SETTINGS_SENSEVOICE_TEST_TITLE -> R.string.settings_sensevoice_test_title
            AppStringId.SETTINGS_SENSEVOICE_TEST_DESCRIPTION -> R.string.settings_sensevoice_test_description
            AppStringId.SETTINGS_SENSEVOICE_TEST_CURRENT_MODEL -> R.string.settings_sensevoice_test_current_model
            AppStringId.SETTINGS_SPEECH_TEST_XUNFEI_ENGINE -> R.string.settings_speech_test_xunfei_engine
            AppStringId.SETTINGS_SPEECH_TEST_CREDENTIALS_REQUIRED -> {
                R.string.settings_speech_test_credentials_required
            }
            AppStringId.SETTINGS_SPEECH_TEST_SAVED_CONFIG_HINT -> R.string.settings_speech_test_saved_config_hint
            AppStringId.SETTINGS_SENSEVOICE_TEST_MODEL_REQUIRED -> R.string.settings_sensevoice_test_model_required
            AppStringId.SETTINGS_SENSEVOICE_TEST_IDLE -> R.string.settings_sensevoice_test_idle
            AppStringId.SETTINGS_SENSEVOICE_TEST_LISTENING -> R.string.settings_sensevoice_test_listening
            AppStringId.SETTINGS_SENSEVOICE_TEST_RESULT_EMPTY -> R.string.settings_sensevoice_test_result_empty
            AppStringId.SETTINGS_SENSEVOICE_TEST_START -> R.string.settings_sensevoice_test_start
            AppStringId.SETTINGS_SENSEVOICE_TEST_STOP -> R.string.settings_sensevoice_test_stop
            AppStringId.SETTINGS_SPEECH_UNDERSTANDING -> R.string.settings_speech_understanding
            AppStringId.SETTINGS_DETECTED_LANGUAGE -> R.string.settings_detected_language
            AppStringId.SETTINGS_DETECTED_EMOTION -> R.string.settings_detected_emotion
            AppStringId.SETTINGS_DETECTED_AUDIO_EVENT -> R.string.settings_detected_audio_event
            AppStringId.SETTINGS_RESULT_PENDING -> R.string.settings_result_pending
            AppStringId.EMOTION_HAPPY -> R.string.emotion_happy
            AppStringId.EMOTION_SAD -> R.string.emotion_sad
            AppStringId.EMOTION_ANGRY -> R.string.emotion_angry
            AppStringId.EMOTION_NEUTRAL -> R.string.emotion_neutral
            AppStringId.EMOTION_FEARFUL -> R.string.emotion_fearful
            AppStringId.EMOTION_DISGUSTED -> R.string.emotion_disgusted
            AppStringId.EMOTION_SURPRISED -> R.string.emotion_surprised
            AppStringId.AUDIO_EVENT_SPEECH -> R.string.audio_event_speech
            AppStringId.AUDIO_EVENT_BGM -> R.string.audio_event_bgm
            AppStringId.AUDIO_EVENT_APPLAUSE -> R.string.audio_event_applause
            AppStringId.AUDIO_EVENT_LAUGHTER -> R.string.audio_event_laughter
            AppStringId.AUDIO_EVENT_CRY -> R.string.audio_event_cry
            AppStringId.AUDIO_EVENT_SNEEZE -> R.string.audio_event_sneeze
            AppStringId.AUDIO_EVENT_BREATH -> R.string.audio_event_breath
            AppStringId.AUDIO_EVENT_COUGH -> R.string.audio_event_cough
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
            AppStringId.SETTINGS_REDACT_PII -> R.string.settings_redact_pii
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
            AppStringId.ERROR_SENSEVOICE_UNAVAILABLE -> R.string.error_sensevoice_unavailable
            AppStringId.ERROR_SENSEVOICE_MODEL_NOT_READY -> R.string.error_sensevoice_model_not_ready
            AppStringId.ERROR_SENSEVOICE_INFERENCE -> R.string.error_sensevoice_inference
            AppStringId.ERROR_SPEECH_RECOGNITION -> R.string.error_speech_recognition
            AppStringId.ERROR_GENERIC -> R.string.error_generic
        }
        return context.getString(resId)
    }
}
