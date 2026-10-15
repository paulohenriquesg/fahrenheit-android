// SharedPreferencesHandler.kt
package com.paulohenriquesg.fahrenheit.storage

import android.content.Context
import androidx.core.content.edit
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.paulohenriquesg.fahrenheit.auth.SessionRole

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

    /** Whether the theme was ever chosen here, as opposed to defaulted. */
    fun hasChosenTheme(): Boolean = sharedPreferences.contains("dark_theme")

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
            dateFormat = sharedPreferences.getString("date_format", null),
            lastUpdateCheck = sharedPreferences.getLong("last_update_check", 0L),
            // -1 stands for "never": SharedPreferences has no nullable Int.
            updateIgnoredVersionCode = sharedPreferences.getInt("update_ignored_version_code", -1).takeIf { it > 0 },
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
            putString("date_format", userPreferences.dateFormat)
            putLong("last_update_check", userPreferences.lastUpdateCheck)
            putInt("update_ignored_version_code", userPreferences.updateIgnoredVersionCode ?: -1)
            // The one-day snooze #229 replaced: dropped wherever it was left.
            remove("update_snooze_version_code")
            remove("update_snooze_at")
            putInt("pending_install_version_code", userPreferences.pendingInstallVersionCode ?: -1)
            putBoolean("update_check_enabled", userPreferences.updateCheckEnabled)
            putString("selected_library_id", userPreferences.selectedLibraryId)
        }
    }

    /**
     * Signing out: the session goes, the settings stay (#63).
     *
     * The tokens are the session, and so is the user's role (#225). The host and username are settings, the theme
     * and layout are preferences, and retyping a server address on a TV keyboard
     * is the most expensive thing this app can ask for. The selected library does
     * go, because it belongs to the account that just left.
     */
    fun clearSession() {
        sharedPreferences.edit {
            remove("token")
            remove("refresh_token")
            remove("selected_library_id")
            remove(KEY_USER_TYPE)
            remove(KEY_MAY_UPDATE)
        }
    }

    /** Forget the theme choice, so the device's setting decides again. */
    fun clearThemeChoice() {
        sharedPreferences.edit {
            remove("dark_theme")
        }
    }

    fun clearPreferences() {
        sharedPreferences.edit {
            clear()
        }
    }

    /**
     * The signed-in user's type and update permission (#225), written at sign-in
     * and on every launch's check of the session; null before either.
     */
    fun getSessionRole(): SessionRole? {
        if (!sharedPreferences.contains(KEY_MAY_UPDATE)) return null
        return SessionRole(sharedPreferences.getString(KEY_USER_TYPE, null), sharedPreferences.getBoolean(KEY_MAY_UPDATE, false))
    }

    fun saveSessionRole(role: SessionRole) {
        sharedPreferences.edit {
            putString(KEY_USER_TYPE, role.type)
            putBoolean(KEY_MAY_UPDATE, role.mayUpdate)
        }
    }

    private companion object {
        const val KEY_USER_TYPE = "user_type"
        const val KEY_MAY_UPDATE = "may_update"
    }

    fun getSelectedLibraryId(): String? {
        return getUserPreferences().selectedLibraryId
    }

    fun saveSelectedLibraryId(libraryId: String) {
        val prefs = getUserPreferences()
        saveUserPreferences(prefs.copy(selectedLibraryId = libraryId))
    }
}