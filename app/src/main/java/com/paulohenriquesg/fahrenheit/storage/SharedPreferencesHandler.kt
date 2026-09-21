// SharedPreferencesHandler.kt
package com.paulohenriquesg.fahrenheit.storage

import android.content.Context
import androidx.core.content.edit
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
            // Deliberately not defaulted to "": an empty string would later be sent
            // as a valid-looking x-refresh-token header.
            refreshToken = sharedPreferences.getString("refresh_token", null),
            darkTheme = sharedPreferences.getBoolean("dark_theme", false),
            isRowLayout = sharedPreferences.getBoolean("is_row_layout", true),
            lastUpdateCheck = sharedPreferences.getLong("last_update_check", 0L),
            // -1 stands for "never": SharedPreferences has no nullable Int.
            updateSnoozeVersionCode = sharedPreferences.getInt("update_snooze_version_code", -1).takeIf { it > 0 },
            updateSnoozeAt = sharedPreferences.getLong("update_snooze_at", -1L).takeIf { it > 0 },
            pendingInstallVersionCode = sharedPreferences.getInt("pending_install_version_code", -1).takeIf { it > 0 },
            updateCheckEnabled = sharedPreferences.getBoolean("update_check_enabled", true),
            selectedLibraryId = sharedPreferences.getString("selected_library_id", null)
        )
    }

    fun saveUserPreferences(userPreferences: UserPreferences) {
        sharedPreferences.edit {
            putString("host", userPreferences.host)
            putString("username", userPreferences.username)
            putString("token", userPreferences.token)
            putString("refresh_token", userPreferences.refreshToken)
            putBoolean("dark_theme", userPreferences.darkTheme)
            putBoolean("is_row_layout", userPreferences.isRowLayout)
            putLong("last_update_check", userPreferences.lastUpdateCheck)
            putInt("update_snooze_version_code", userPreferences.updateSnoozeVersionCode ?: -1)
            putLong("update_snooze_at", userPreferences.updateSnoozeAt ?: -1L)
            putInt("pending_install_version_code", userPreferences.pendingInstallVersionCode ?: -1)
            putBoolean("update_check_enabled", userPreferences.updateCheckEnabled)
            putString("selected_library_id", userPreferences.selectedLibraryId)
        }
    }

    fun clearPreferences() {
        sharedPreferences.edit {
            clear()
        }
    }

    fun getSelectedLibraryId(): String? {
        return getUserPreferences().selectedLibraryId
    }

    fun saveSelectedLibraryId(libraryId: String) {
        val prefs = getUserPreferences()
        saveUserPreferences(prefs.copy(selectedLibraryId = libraryId))
    }
}