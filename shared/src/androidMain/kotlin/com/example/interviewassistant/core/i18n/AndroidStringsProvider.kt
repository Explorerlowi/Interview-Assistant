package com.example.interviewassistant.core.i18n

import android.content.Context
import com.example.interviewassistant.shared.R

/**
 * Android [StringsProvider] backed by shared module string resources.
 */
class AndroidStringsProvider(private val context: Context) : StringsProvider {
    override fun get(id: AppStringId): String {
        val resId = when (id) {
            AppStringId.COMMON_OK -> R.string.common_ok
            AppStringId.COMMON_CANCEL -> R.string.common_cancel
            AppStringId.LOGIN_TITLE -> R.string.login_title
            AppStringId.LOGIN_USERNAME -> R.string.login_username
            AppStringId.LOGIN_BTN -> R.string.login_btn
        }
        return context.getString(resId)
    }
}
