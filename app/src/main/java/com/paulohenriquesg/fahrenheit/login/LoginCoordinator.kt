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
 * Two ways in: username and password, or an API key created in the
 * Audiobookshelf web UI. The second exists for accounts with no password at
 * all (#2) - those users could not get past the password field before.
 * Both share the same validate / persist / activate tail so they cannot drift.
 *
 * @param performLogin posts username and password.
 * @param performApiKeyLogin resolves the user behind an API key.
 * @param activateSession installs the stored session, returning whether it is
 *   usable.
 */
class LoginCoordinator(
    private val sessionManager: SessionManager,
    private val performLogin: suspend (host: String, username: String, password: String) -> AuthSession,
    private val performApiKeyLogin: suspend (host: String, apiKey: String) -> AuthSession,
    private val activateSession: () -> SessionState
) {
    suspend fun login(host: String, username: String, password: String): LoginOutcome {
        if (host.isBlank() || username.isBlank() || password.isBlank()) {
            return LoginOutcome.Invalid("All fields are required")
        }
        hostProblem(host)?.let { return it }
        return complete(host) { performLogin(host, username, password) }
    }

    suspend fun loginWithApiKey(host: String, apiKey: String): LoginOutcome {
        // Keys are pasted from the web UI; a stray newline or space would be
        // sent verbatim and fail as an invalid token.
        val key = apiKey.trim()
        if (host.isBlank() || key.isEmpty()) {
            return LoginOutcome.Invalid("Host and API key are required")
        }
        hostProblem(host)?.let { return it }
        return complete(host) { performApiKeyLogin(host, key) }
    }

    private fun hostProblem(host: String): LoginOutcome.Invalid? =
        if (!host.startsWith("http://") && !host.startsWith("https://")) {
            LoginOutcome.Invalid("Host must start with http:// or https://")
        } else {
            null
        }

    private suspend fun complete(host: String, attempt: suspend () -> AuthSession): LoginOutcome {
        val session = try {
            attempt()
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
