package com.paulohenriquesg.fahrenheit.update

import android.content.Context
import com.paulohenriquesg.fahrenheit.storage.SharedPreferencesHandler

/** The snooze, kept where the rest of the user's preferences live. */
class PreferencesSnoozeStore(context: Context) : SnoozeStore {
    private val handler = SharedPreferencesHandler(context)

    override fun snoozedVersionCode(): Int? =
        handler.getUserPreferences().updateSnoozeVersionCode

    override fun snoozedAt(): Long? = handler.getUserPreferences().updateSnoozeAt

    override fun snooze(versionCode: Int, at: Long) {
        val preferences = handler.getUserPreferences()
        handler.saveUserPreferences(
            preferences.copy(updateSnoozeVersionCode = versionCode, updateSnoozeAt = at)
        )
    }
}
