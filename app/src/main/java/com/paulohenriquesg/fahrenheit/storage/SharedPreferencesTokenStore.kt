package com.paulohenriquesg.fahrenheit.storage

import com.paulohenriquesg.fahrenheit.api.StoredCredentials
import com.paulohenriquesg.fahrenheit.api.TokenStore

/**
 * The real [TokenStore], backed by encrypted SharedPreferences.
 *
 * Writes through `copy` so signing in again does not reset the user's library
 * or display choices, which live in the same preferences object.
 */
class SharedPreferencesTokenStore(
    private val preferences: SharedPreferencesHandler
) : TokenStore {

    override fun read(): StoredCredentials {
        val prefs = preferences.getUserPreferences()
        return StoredCredentials(
            host = prefs.host,
            username = prefs.username,
            accessToken = prefs.token,
            refreshToken = prefs.refreshToken
        )
    }

    override fun write(credentials: StoredCredentials) {
        val updated = preferences.getUserPreferences().copy(
            host = credentials.host,
            username = credentials.username,
            token = credentials.accessToken,
            refreshToken = credentials.refreshToken
        )
        preferences.saveUserPreferences(updated)
    }
}
