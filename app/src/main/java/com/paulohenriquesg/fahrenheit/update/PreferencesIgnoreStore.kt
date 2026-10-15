package com.paulohenriquesg.fahrenheit.update

import android.content.Context
import com.paulohenriquesg.fahrenheit.storage.SharedPreferencesHandler

/** The ignored version, kept where the rest of the user's preferences live. */
class PreferencesIgnoreStore(context: Context) : IgnoreStore {
    private val handler = SharedPreferencesHandler(context)

    override fun ignoredVersionCode(): Int? =
        handler.getUserPreferences().updateIgnoredVersionCode

    override fun ignore(versionCode: Int) {
        val preferences = handler.getUserPreferences()
        handler.saveUserPreferences(preferences.copy(updateIgnoredVersionCode = versionCode))
    }
}
