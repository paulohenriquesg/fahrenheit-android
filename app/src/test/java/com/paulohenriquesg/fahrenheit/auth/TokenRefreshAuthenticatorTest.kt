package com.paulohenriquesg.fahrenheit.auth

import android.content.Context
import com.paulohenriquesg.fahrenheit.api.AuthSession
import com.paulohenriquesg.fahrenheit.api.SessionManager
import com.paulohenriquesg.fahrenheit.api.TokenRefreshAuthenticator
import com.paulohenriquesg.fahrenheit.storage.SharedPreferencesHandler
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Audiobookshelf access tokens last an hour. Without this, the app dies mid-session
 * and the user is bounced to the login screen.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class TokenRefreshAuthenticatorTest {
    private lateinit var sessionManager: SessionManager

    @Before
    fun setup() {
        val context: Context = RuntimeEnvironment.getApplication()
        val prefs = SharedPreferencesHandler(context)
        prefs.clearPreferences()
        sessionManager = SessionManager(prefs)
    }

    private fun unauthorized(prior: Response? = null): Response {
        val request = Request.Builder().url("http://abs.local/api/libraries").build()
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .apply { if (prior != null) priorResponse(prior) }
            .build()
    }

    private fun signedIn(refreshToken: String?) {
        sessionManager.persist("http://abs.local", AuthSession("stale", refreshToken, "testuser"))
    }

    @Test
    fun `a 401 is retried with a freshly minted access token`() {
        signedIn(refreshToken = "refresh")

        val authenticator = TokenRefreshAuthenticator(sessionManager) {
            AuthSession("brand-new-access", "rotated-refresh", "testuser")
        }

        val retry = authenticator.authenticate(null, unauthorized())

        assertEquals("Bearer brand-new-access", retry?.header("Authorization"))
    }

    @Test
    fun `the rotated tokens are persisted for the next request`() {
        signedIn(refreshToken = "refresh")

        val authenticator = TokenRefreshAuthenticator(sessionManager) {
            AuthSession("brand-new-access", "rotated-refresh", "testuser")
        }
        authenticator.authenticate(null, unauthorized())

        assertEquals("brand-new-access", sessionManager.accessToken())
        assertEquals("rotated-refresh", sessionManager.refreshToken())
    }

    @Test
    fun `it gives up when there is no refresh token to spend`() {
        // Pre-2.26 servers issue only the legacy non-expiring token.
        signedIn(refreshToken = null)

        val authenticator = TokenRefreshAuthenticator(sessionManager) {
            error("must not attempt a refresh without a refresh token")
        }

        assertNull(authenticator.authenticate(null, unauthorized()))
    }

    @Test
    fun `it retries only once so a rejecting server cannot spin forever`() {
        signedIn(refreshToken = "refresh")
        var attempts = 0

        val authenticator = TokenRefreshAuthenticator(sessionManager) {
            attempts++
            AuthSession("brand-new-access", "rotated-refresh", "testuser")
        }

        val alreadyRetried = unauthorized(prior = unauthorized())

        assertNull(authenticator.authenticate(null, alreadyRetried))
        assertEquals(0, attempts)
    }

    @Test
    fun `a failed refresh surfaces as giving up rather than throwing`() {
        signedIn(refreshToken = "expired-refresh")

        val authenticator = TokenRefreshAuthenticator(sessionManager) {
            throw java.io.IOException("refresh token expired")
        }

        assertNull(authenticator.authenticate(null, unauthorized()))
    }
}
