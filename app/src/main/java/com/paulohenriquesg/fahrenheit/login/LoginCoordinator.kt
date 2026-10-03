package com.paulohenriquesg.fahrenheit.login

import com.paulohenriquesg.fahrenheit.auth.AuthSession
import com.paulohenriquesg.fahrenheit.auth.SessionManager
import com.paulohenriquesg.fahrenheit.auth.SessionState
import retrofit2.HttpException
import java.io.IOException

/** What signing in produced, so the UI only has to decide how to say it. */
sealed interface LoginOutcome {
    data object Success : LoginOutcome

    /**
     * @param detail what the exception said, for the log only: the screen says
     *   what to do next instead of quoting it.
     */
    data class Failed(val error: LoginError, val detail: String? = null) : LoginOutcome
}

/** The form's inputs, so an error can sit beside the one that caused it. */
enum class LoginField { Host, Username, Password, ApiKey }

/**
 * Why signing in did not work, typed rather than worded: the screen turns
 * each one into an instruction and draws it beside [field] (#99).
 */
sealed class LoginError(val field: LoginField) {
    data object HostMissing : LoginError(LoginField.Host)
    data object HostScheme : LoginError(LoginField.Host)
    data object UsernameMissing : LoginError(LoginField.Username)
    data object ApiKeyMissing : LoginError(LoginField.ApiKey)

    /**
     * The server answered 401/403. Audiobookshelf says the same for an unknown
     * username, so this cannot claim the username was right.
     */
    data object PasswordRejected : LoginError(LoginField.Password)
    data object ApiKeyRejected : LoginError(LoginField.ApiKey)

    /** No answer at all: wrong address, server down, or no network. */
    data object Unreachable : LoginError(LoginField.Host)
    data class ServerError(val code: Int) : LoginError(LoginField.Host)
    data object Unexpected : LoginError(LoginField.Host)

    /** Credentials were accepted but the resulting session is unusable. */
    data object UnusableSession : LoginError(LoginField.Host)
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
        // No check on the password: Audiobookshelf accounts can have none, and
        // its own apps sign those users in with the field left empty (#2).
        hostProblem(host)?.let { return LoginOutcome.Failed(it) }
        if (username.isBlank()) return LoginOutcome.Failed(LoginError.UsernameMissing)
        return complete(host, LoginError.PasswordRejected) { performLogin(host, username, password) }
    }

    suspend fun loginWithApiKey(host: String, apiKey: String): LoginOutcome {
        // Keys are pasted from the web UI; a stray newline or space would be
        // sent verbatim and fail as an invalid token.
        val key = apiKey.trim()
        hostProblem(host)?.let { return LoginOutcome.Failed(it) }
        if (key.isEmpty()) return LoginOutcome.Failed(LoginError.ApiKeyMissing)
        return complete(host, LoginError.ApiKeyRejected) { performApiKeyLogin(host, key) }
    }

    /** @param rejected what a 401/403 means for the credential just sent. */
    private suspend fun complete(
        host: String,
        rejected: LoginError,
        attempt: suspend () -> AuthSession
    ): LoginOutcome {
        val session = try {
            attempt()
        } catch (e: HttpException) {
            val error = if (e.code() == 401 || e.code() == 403) rejected else LoginError.ServerError(e.code())
            return LoginOutcome.Failed(error, e.message)
        } catch (e: IOException) {
            return LoginOutcome.Failed(LoginError.Unreachable, e.message)
        } catch (e: Exception) {
            return LoginOutcome.Failed(LoginError.Unexpected, e.message)
        }

        sessionManager.persist(host, session)

        // Storing is not the same as working: if the session will not activate,
        // navigating onwards just bounces the user straight back here.
        if (activateSession() == SessionState.NeedsLogin) {
            return LoginOutcome.Failed(LoginError.UnusableSession)
        }

        return LoginOutcome.Success
    }

    companion object {
        /** What is wrong with an address before anything is sent to it, if anything. */
        fun hostProblem(host: String): LoginError? = when {
            host.isBlank() -> LoginError.HostMissing
            !host.startsWith("http://") && !host.startsWith("https://") -> LoginError.HostScheme
            else -> null
        }
    }
}
