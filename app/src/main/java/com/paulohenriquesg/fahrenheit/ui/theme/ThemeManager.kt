package com.paulohenriquesg.fahrenheit.ui.theme

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.mutableStateOf
import com.paulohenriquesg.fahrenheit.storage.SharedPreferencesHandler

object ThemeManager {
    private var _isDarkTheme = mutableStateOf(false)
    val isDarkTheme = _isDarkTheme

    fun initialize(context: Context) {
        val sharedPreferencesHandler = SharedPreferencesHandler(context)
        val chosen = sharedPreferencesHandler.getUserPreferences().darkTheme
            .takeIf { sharedPreferencesHandler.hasChosenTheme() }
        _isDarkTheme.value = ThemeChoice.resolve(chosen, systemIsDark = context.isSystemDark())
    }

    private fun Context.isSystemDark(): Boolean =
        resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
            Configuration.UI_MODE_NIGHT_YES

    fun toggleTheme(context: Context) {
        _isDarkTheme.value = !_isDarkTheme.value
        val sharedPreferencesHandler = SharedPreferencesHandler(context)
        sharedPreferencesHandler.saveUserPreferences(
            sharedPreferencesHandler.getUserPreferences().copy(darkTheme = _isDarkTheme.value)
        )
    }

    /** What settings should show as chosen. */
    fun preference(context: Context): ThemePreference {
        val handler = SharedPreferencesHandler(context)
        val stored = handler.getUserPreferences().darkTheme.takeIf { handler.hasChosenTheme() }
        return ThemePreference.of(stored)
    }

    /**
     * Apply a choice. System removes the stored one rather than storing a third
     * value, so the device decides again - now and on every later launch.
     */
    fun apply(context: Context, preference: ThemePreference) {
        val handler = SharedPreferencesHandler(context)
        val stored = preference.stored()
        if (stored == null) {
            handler.clearThemeChoice()
            _isDarkTheme.value = preference.isDark(context.isSystemDark())
        } else {
            handler.saveUserPreferences(handler.getUserPreferences().copy(darkTheme = stored))
            _isDarkTheme.value = stored
        }
    }

    fun setTheme(context: Context, isDark: Boolean) {
        _isDarkTheme.value = isDark
        val sharedPreferencesHandler = SharedPreferencesHandler(context)
        sharedPreferencesHandler.saveUserPreferences(
            sharedPreferencesHandler.getUserPreferences().copy(darkTheme = isDark)
        )
    }
}
