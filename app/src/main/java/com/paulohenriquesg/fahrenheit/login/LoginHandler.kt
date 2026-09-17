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

class LoginHandler(private val context: Context) {
    private val sessionManager = SessionManager(SharedPreferencesHandler(context))
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun handleLogin(
        host: String,
        username: String,
        password: String,
        isLoading: MutableState<Boolean>
    ) {
        if (host.isBlank() || username.isBlank() || password.isBlank()) {
            Toast.makeText(context, "All fields are required", Toast.LENGTH_SHORT).show()
            return
        }

        if (!host.startsWith("http://") && !host.startsWith("https://")) {
            Toast.makeText(context, "Host must start with http:// or https://", Toast.LENGTH_SHORT)
                .show()
            return
        }

        isLoading.value = true
        scope.launch {
            try {
                val session = withContext(Dispatchers.IO) {
                    AuthRepository(ApiClient.createAuthApi(host)).login(username, password, host)
                }

                sessionManager.persist(host, session)
                ApiClient.initialize(context)

                Toast.makeText(context, "Login successful", Toast.LENGTH_SHORT).show()

                context.startActivity(Intent(context, MainActivity::class.java))
                if (context is LoginActivity) {
                    context.finish()
                }
            } catch (e: Exception) {
                Log.e("LoginHandler", "Login failed", e)
                Toast.makeText(context, "Login failed: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading.value = false
            }
        }
    }
}
