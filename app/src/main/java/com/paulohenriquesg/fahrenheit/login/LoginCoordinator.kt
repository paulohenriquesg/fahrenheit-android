package com.paulohenriquesg.fahrenheit.login

import com.paulohenriquesg.fahrenheit.api.AuthSession
import com.paulohenriquesg.fahrenheit.api.SessionManager
import com.paulohenriquesg.fahrenheit.api.SessionState

/** What signing in produced, so the UI only has to decide how to say it. */
sealed interface LoginOutcome {
    data object Success : LoginOutcome

    /** Rejected locally; no request was made. */
    data class Invalid(val message: String) : LoginOutcome

    /** The server or the network refused. */
    data class Failed(val message: String) : LoginOutcome

    /** Credentials were accepted but the resulting session is unusable. */
    data object UnusableSession : LoginOutcome
}

/**
 * The decisions behind signing in, with no Android UI attached.
 *
 * This used to live inside LoginHandler alongside toasts, Intents and a
 * Context, which meant none of it could be tested without driving the UI - the
 * login package sat at 0% coverage.
 *
 * @param performLogin does the network call. Injected so tests can drive every
 *   branch, and so the password requirement can later depend on what the server
 *   reports via /status (see #2) rather than being assumed here.
 * @param activateSession installs the stored session, returning whether it is
 *   usable.
 */
class LoginCoordinator(
    private val sessionManager: SessionManager,
    private val performLogin: suspend (host: String, username: String, password: String) -> AuthSession,
    private val activateSession: () -> SessionState
) {
    suspend fun login(host: String, username: String, password: String): LoginOutcome {
        if (host.isBlank() || username.isBlank() || password.isBlank()) {
            return LoginOutcome.Invalid("All fields are required")
        }
        if (!host.startsWith("http://") && !host.startsWith("https://")) {
            return LoginOutcome.Invalid("Host must start with http:// or https://")
        }

        val session = try {
            performLogin(host, username, password)
        } catch (e: Exception) {
            return LoginOutcome.Failed(e.message ?: "Login failed")
        }

        sessionManager.persist(host, session)

        // Storing is not the same as working: if the session will not activate,
        // navigating onwards just bounces the user straight back here.
        if (activateSession() == SessionState.NeedsLogin) {
            return LoginOutcome.UnusableSession
        }

        return LoginOutcome.Success
    }
}
