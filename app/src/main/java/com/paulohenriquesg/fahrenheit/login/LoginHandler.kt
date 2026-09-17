package com.paulohenriquesg.fahrenheit.login

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.MutableState
import com.paulohenriquesg.fahrenheit.api.ApiClient
import com.paulohenriquesg.fahrenheit.api.AuthRepository
import com.paulohenriquesg.fahrenheit.api.SessionManager
import com.paulohenriquesg.fahrenheit.main.MainActivity
import com.paulohenriquesg.fahrenheit.storage.SharedPreferencesHandler
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
        sessionManager = SessionManager(SharedPreferencesHandler(context)),
        performLogin = { host, username, password ->
            withContext(Dispatchers.IO) {
                AuthRepository(ApiClient.createAuthApi(host)).login(username, password, host)
            }
        },
        activateSession = { ApiClient.initialize(context) }
    )

    fun handleLogin(
        host: String,
        username: String,
        password: String,
        isLoading: MutableState<Boolean>
    ) {
        isLoading.value = true
        scope.launch {
            try {
                when (val outcome = coordinator.login(host, username, password)) {
                    is LoginOutcome.Success -> {
                        Toast.makeText(context, "Login successful", Toast.LENGTH_SHORT).show()
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
                            "Signed in, but the server address could not be used",
                            Toast.LENGTH_LONG
                        ).show()
                }
            } finally {
                isLoading.value = false
            }
        }
    }
}
