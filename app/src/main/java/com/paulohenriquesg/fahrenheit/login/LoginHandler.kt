package com.paulohenriquesg.fahrenheit.login

import android.content.Context
import com.paulohenriquesg.fahrenheit.R
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.MutableState
import com.paulohenriquesg.fahrenheit.api.ApiClient
import com.paulohenriquesg.fahrenheit.api.AuthRepository
import com.paulohenriquesg.fahrenheit.api.SessionManager
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
class LoginHandler(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val coordinator = LoginCoordinator(
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
            is LoginOutcome.Success -> {
                Toast.makeText(context, context.getString(R.string.login_successful), Toast.LENGTH_SHORT).show()
                context.startActivity(Intent(context, MainActivity::class.java))
                if (context is LoginActivity) context.finish()
            }

            is LoginOutcome.Invalid ->
                Toast.makeText(context, outcome.message, Toast.LENGTH_SHORT).show()

            is LoginOutcome.Failed -> {
                Log.e("LoginHandler", "Login failed: ${outcome.message}")
                Toast.makeText(
                    context,
                    "Login failed: ${outcome.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }

            is LoginOutcome.UnusableSession ->
                Toast.makeText(
                    context,
                    context.getString(R.string.signed_in_but_host_missing),
                    Toast.LENGTH_LONG
                ).show()
        }
    }
}
