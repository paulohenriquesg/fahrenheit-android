// SharedPreferencesHandler.kt
package com.paulohenriquesg.fahrenheit.storage

import android.content.Context
import android.content.SharedPreferences

class SharedPreferencesHandler(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    fun getUserPreferences(): UserPreferences {
        return UserPreferences(
            host = sharedPreferences.getString("host", "") ?: "",
            username = sharedPreferences.getString("username", "") ?: "",
            token = sharedPreferences.getString("token", "") ?: "",
            darkTheme = sharedPreferences.getBoolean("dark_theme", false)
        )
    }

    fun saveUserPreferences(userPreferences: UserPreferences) {
        with(sharedPreferences.edit()) {
            putString("host", userPreferences.host)
            putString("username", userPreferences.username)
            putString("token", userPreferences.token)
            putBoolean("dark_theme", userPreferences.darkTheme)
            apply()
        }
    }

    fun clearPreferences() {
        with(sharedPreferences.edit()) {
            clear()
            apply()
        }
    }
}