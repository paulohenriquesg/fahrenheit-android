// SharedPreferencesHandler.kt
package com.paulohenriquesg.fahrenheit.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SharedPreferencesHandler(context: Context) {
    private val sharedPreferences: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "encrypted_app_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        // Fallback to regular SharedPreferences if encryption fails
        android.util.Log.e("SharedPreferencesHandler", "Failed to create encrypted preferences, falling back to standard", e)
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    }

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