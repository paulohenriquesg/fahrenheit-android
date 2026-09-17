package com.paulohenriquesg.fahrenheit.auth

import android.content.Context
import com.paulohenriquesg.fahrenheit.api.AuthSession
import com.paulohenriquesg.fahrenheit.api.SessionManager
import com.paulohenriquesg.fahrenheit.api.SessionState
import com.paulohenriquesg.fahrenheit.login.LoginCoordinator
import com.paulohenriquesg.fahrenheit.login.LoginOutcome
import com.paulohenriquesg.fahrenheit.storage.SharedPreferencesHandler
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.IOException

/**
 * The decisions behind signing in, separated from the Activity that shows them.
 *
 * LoginHandler needed a Context, toasts and an Activity stack to run at all, so
 * none of this had coverage - the login package sat at 0%. Everything here used
 * to be reachable only by driving the UI.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class LoginCoordinatorTest {
    private lateinit var sessionManager: SessionManager
    private lateinit var prefs: SharedPreferencesHandler

    @Before
    fun setup() {
        val context: Context = RuntimeEnvironment.getApplication()
        prefs = SharedPreferencesHandler(context)
        prefs.clearPreferences()
        sessionManager = SessionManager(prefs)
    }

    private fun coordinator(
        login: suspend (String, String, String) -> AuthSession = { _, _, _ ->
            AuthSession("access", "refresh", "testuser")
        },
        activate: () -> SessionState = { SessionState.Ready }
    ) = LoginCoordinator(sessionManager, login, activate)

    @Test
    fun `a blank field is rejected before any request is made`() = runBlocking {
        var called = false
        val c = coordinator(login = { _, _, _ -> called = true; error("must not be called") })

        val outcome = c.login("http://abs.local", "", "hunter2")

        assertTrue(outcome is LoginOutcome.Invalid)
        assertEquals(false, called)
    }

    @Test
    fun `a host without a scheme is rejected before any request is made`() = runBlocking {
        var called = false
        val c = coordinator(login = { _, _, _ -> called = true; error("must not be called") })

        val outcome = c.login("abs.local:13378", "testuser", "hunter2")

        assertTrue(outcome is LoginOutcome.Invalid)
        assertEquals(false, called)
    }

    @Test
    fun `a successful sign-in stores the session`() = runBlocking {
        val outcome = coordinator().login("http://abs.local", "testuser", "hunter2")

        assertEquals(LoginOutcome.Success, outcome)
        assertEquals("access", sessionManager.accessToken())
        assertEquals("refresh", sessionManager.refreshToken())
        assertEquals("http://abs.local", sessionManager.host())
    }

    @Test
    fun `a network failure is reported, not thrown`() = runBlocking {
        val c = coordinator(login = { _, _, _ -> throw IOException("connection refused") })

        val outcome = c.login("http://abs.local", "testuser", "hunter2")

        assertTrue(outcome is LoginOutcome.Failed)
        assertTrue((outcome as LoginOutcome.Failed).message.contains("connection refused"))
    }

    @Test
    fun `credentials that store but do not activate are reported distinctly`() = runBlocking {
        // Otherwise the app navigates to Main and bounces straight back to login
        // with no explanation.
        val c = coordinator(activate = { SessionState.NeedsLogin })

        assertEquals(LoginOutcome.UnusableSession, c.login("http://abs.local", "testuser", "hunter2"))
    }

    @Test
    fun `an https host is accepted`() = runBlocking {
        assertEquals(
            LoginOutcome.Success,
            coordinator().login("https://abs.example.com", "testuser", "hunter2")
        )
    }

    @Test
    fun `a failed sign-in leaves no session behind`() = runBlocking {
        val c = coordinator(login = { _, _, _ -> throw IOException("nope") })

        c.login("http://abs.local", "testuser", "hunter2")

        assertEquals(null, sessionManager.refreshToken())
    }
}
