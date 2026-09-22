package com.paulohenriquesg.fahrenheit.auth

import android.util.Log
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * Exchanges an expired access token for a new one when the server answers 401.
 *
 * Audiobookshelf access tokens last an hour; without this the app fails mid-session
 * and the user is bounced back to the login screen.
 *
 * @param refreshSession performs the refresh. Blocking on purpose - OkHttp calls
 *   [authenticate] on a worker thread that is already waiting on this request.
 */
class TokenRefreshAuthenticator(
    private val sessionManager: SessionManager,
    private val refreshSession: (String) -> AuthSession
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // Already retried once. Retrying again would spin against a server that
        // rejects the credentials outright.
        if (response.priorResponse != null) return null

        // Servers older than 2.26 issue only the legacy non-expiring token, so a
        // 401 there is a genuine auth failure and there is nothing to spend.
        val refreshToken = sessionManager.refreshToken() ?: return null

        val session = try {
            refreshSession(refreshToken)
        } catch (e: Exception) {
            Log.w("TokenRefreshAuthenticator", "Refresh failed; giving up", e)
            return null
        }

        // Keep the token we spent if the response carried no replacement.
        // Storing null there would leave nothing to refresh with, and the next
        // expiry would drop the user at the login screen. Servers do rotate it
        // and do send the new one (checked against 2.36.0), so this is a guard,
        // not the usual path. A fresh sign-in still replaces it outright, which
        // is how signing in with an API key clears it.
        sessionManager.persist(
            sessionManager.host(),
            session.copy(refreshToken = session.refreshToken ?: refreshToken)
        )

        return response.request.newBuilder()
            .header("Authorization", "Bearer ${session.accessToken}")
            .build()
    }
}
