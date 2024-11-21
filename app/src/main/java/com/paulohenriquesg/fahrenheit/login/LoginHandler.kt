package com.paulohenriquesg.fahrenheit.login

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.MutableState
import com.paulohenriquesg.fahrenheit.api.ApiClient
import com.paulohenriquesg.fahrenheit.api.LoginRequest
import com.paulohenriquesg.fahrenheit.api.LoginResponse
import com.paulohenriquesg.fahrenheit.main.MainActivity
import com.paulohenriquesg.fahrenheit.storage.SharedPreferencesHandler
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginHandler(private val context: Context) {
    private val sharedPreferencesHandler = SharedPreferencesHandler(context)

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
        val loginRequest = LoginRequest(username, password, host)
        val apiService = ApiClient.getApiServiceForLogin(host)
        apiService.login(loginRequest).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                isLoading.value = false
                if (response.isSuccessful) {
                    val loginResponse = response.body()
                    // Handle successful login
                    if (loginResponse != null) {
                        saveToLocalStorage(host, loginResponse.user.username, loginResponse.user.token)
                        Toast.makeText(context, "Login successful", Toast.LENGTH_SHORT).show()

                        ApiClient.initialize(context)

                        // Redirect to MainActivity
                        val intent = Intent(context, MainActivity::class.java)
                        context.startActivity(intent)
                        if (context is LoginActivity) {
                            context.finish()
                        }
                    }
                } else {
                    // Handle login failure
                    Toast.makeText(
                        context,
                        "Login failed: ${response.message()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                isLoading.value = false
                // Log the error message
                Log.e("LoginHandler", "Network error", t)
                Toast.makeText(
                    context,
                    "Network error: ${t.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun saveToLocalStorage(host: String, username: String, token: String) {
        val userPreferences = sharedPreferencesHandler.getUserPreferences().copy(
            host = host,
            username = username,
            token = token
        )
        sharedPreferencesHandler.saveUserPreferences(userPreferences)
    }
}