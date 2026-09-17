package com.paulohenriquesg.fahrenheit.api

import com.paulohenriquesg.fahrenheit.storage.SharedPreferencesHandler

/**
 * Single owner of the signed-in token state.
 *
 * Both the login flow and the 401 refresh path write through here, so the stored
 * access token and refresh token cannot drift apart.
 */
class SessionManager(private val preferences: SharedPreferencesHandler) {

    /** Writes the tokens without disturbing the user's unrelated settings. */
    fun persist(host: String, session: AuthSession) {
        val updated = preferences.getUserPreferences().copy(
            host = host,
            username = session.username,
            token = session.accessToken,
            refreshToken = session.refreshToken
        )
        preferences.saveUserPreferences(updated)
    }

    /** Null when the server issued no refresh token, i.e. there is nothing to retry with. */
    fun refreshToken(): String? = preferences.getUserPreferences().refreshToken

    fun accessToken(): String? = preferences.getUserPreferences().token.takeIf { it.isNotEmpty() }

    fun host(): String = preferences.getUserPreferences().host
}
