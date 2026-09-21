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

    fun setTheme(context: Context, isDark: Boolean) {
        _isDarkTheme.value = isDark
        val sharedPreferencesHandler = SharedPreferencesHandler(context)
        sharedPreferencesHandler.saveUserPreferences(
            sharedPreferencesHandler.getUserPreferences().copy(darkTheme = isDark)
        )
    }
}
