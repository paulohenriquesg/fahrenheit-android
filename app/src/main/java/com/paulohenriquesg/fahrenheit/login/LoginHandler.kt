package com.paulohenriquesg.fahrenheit.login

import android.content.Context
import com.paulohenriquesg.fahrenheit.R
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.paulohenriquesg.fahrenheit.api.ApiClient
import com.paulohenriquesg.fahrenheit.auth.AuthRepository
import com.paulohenriquesg.fahrenheit.auth.SessionManager
import com.paulohenriquesg.fahrenheit.main.MainActivity
import com.paulohenriquesg.fahrenheit.storage.SharedPreferencesHandler
import com.paulohenriquesg.fahrenheit.storage.SharedPreferencesTokenStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Presents [LoginCoordinator]'s result. Deliberately thin: everything worth
 * testing lives in the coordinator, which needs no Context.
 */
class LoginHandler(
    private val context: Context,
    private val coordinator: LoginCoordinator = defaultCoordinator(context)
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _error = mutableStateOf<LoginError?>(null)

    /**
     * Why the last attempt failed. The screen draws it beside the field it
     * concerns; a Toast sat under the keyboard, where nobody saw it (#99).
     */
    val error: State<LoginError?> get() = _error

    fun handleLogin(
        host: String,
        username: String,
        password: String,
        isLoading: MutableState<Boolean>
    ) = run(isLoading) { coordinator.login(host, username, password) }

    /** For accounts with no password: sign in with a key from the web UI. */
    fun handleApiKeyLogin(
        host: String,
        apiKey: String,
        isLoading: MutableState<Boolean>
    ) = run(isLoading) { coordinator.loginWithApiKey(host, apiKey) }

    private fun run(isLoading: MutableState<Boolean>, attempt: suspend () -> LoginOutcome) {
        isLoading.value = true
        _error.value = null
        scope.launch {
            try {
                present(attempt())
            } finally {
                isLoading.value = false
            }
        }
    }

    private fun present(outcome: LoginOutcome) {
        when (outcome) {
            // Home is the confirmation; a Toast on top of it said so late.
            is LoginOutcome.Success -> {
                context.startActivity(Intent(context, MainActivity::class.java))
                if (context is LoginActivity) context.finish()
            }

            is LoginOutcome.Failed -> {
                Log.e("LoginHandler", "Login failed: ${outcome.error} ${outcome.detail.orEmpty()}")
                _error.value = outcome.error
            }
        }
    }

    private companion object {
        fun defaultCoordinator(context: Context) = LoginCoordinator(
            sessionManager = SessionManager(SharedPreferencesTokenStore(SharedPreferencesHandler(context))),
            performLogin = { host, username, password ->
                withContext(Dispatchers.IO) {
                    AuthRepository(ApiClient.createAuthApi(host)).login(username, password, host)
                }
            },
            performApiKeyLogin = { host, apiKey ->
                withContext(Dispatchers.IO) {
                    AuthRepository(ApiClient.createAuthApi(host)).signInWithApiKey(apiKey)
                }
            },
            activateSession = { ApiClient.initialize(context) }
        )
    }
}
