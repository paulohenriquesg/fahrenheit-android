package com.paulohenriquesg.fahrenheit.auth

import android.content.Context
import com.paulohenriquesg.fahrenheit.api.AuthSession
import com.paulohenriquesg.fahrenheit.api.SessionManager
import com.paulohenriquesg.fahrenheit.storage.SharedPreferencesHandler
import com.paulohenriquesg.fahrenheit.storage.UserPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Owns the signed-in token state. Both the login flow and the 401 refresh path
 * write through here, so they cannot drift apart.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SessionManagerTest {
    private lateinit var context: Context
    private lateinit var prefs: SharedPreferencesHandler
    private lateinit var sessionManager: SessionManager

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        prefs = SharedPreferencesHandler(context)
        prefs.clearPreferences()
        sessionManager = SessionManager(prefs)
    }

    @Test
    fun `persisting a session stores both tokens and the identity`() {
        sessionManager.persist(
            host = "http://abs.local",
            session = AuthSession("access", "refresh", "testuser")
        )

        val stored = prefs.getUserPreferences()
        assertEquals("http://abs.local", stored.host)
        assertEquals("testuser", stored.username)
        assertEquals("access", stored.token)
        assertEquals("refresh", stored.refreshToken)
    }

    @Test
    fun `persisting a session leaves unrelated preferences alone`() {
        // Signing in again must not reset the user's library or display choices.
        prefs.saveUserPreferences(
            UserPreferences(
                host = "http://abs.local",
                username = "testuser",
                token = "old",
                darkTheme = true,
                isRowLayout = false,
                selectedLibraryId = "library-456"
            )
        )

        sessionManager.persist(
            host = "http://abs.local",
            session = AuthSession("new-access", "new-refresh", "testuser")
        )

        val stored = prefs.getUserPreferences()
        assertEquals("library-456", stored.selectedLibraryId)
        assertTrue(stored.darkTheme)
        assertEquals(false, stored.isRowLayout)
        assertEquals("new-access", stored.token)
    }

    @Test
    fun `the refresh token is readable for the 401 retry path`() {
        sessionManager.persist(
            host = "http://abs.local",
            session = AuthSession("access", "refresh", "testuser")
        )

        assertEquals("refresh", sessionManager.refreshToken())
    }

    @Test
    fun `a server that issues no refresh token leaves nothing to refresh with`() {
        sessionManager.persist(
            host = "http://abs.local",
            session = AuthSession("legacy-non-expiring", null, "testuser")
        )

        assertNull(sessionManager.refreshToken())
    }
}
