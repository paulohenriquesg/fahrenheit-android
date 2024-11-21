package com.paulohenriquesg.fahrenheit.login

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.paulohenriquesg.fahrenheit.MainActivity
import com.paulohenriquesg.fahrenheit.storage.SharedPreferencesHandler
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme

class LoginActivity : ComponentActivity() {
    private lateinit var loginHandler: LoginHandler

    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        loginHandler = LoginHandler(this)

        // Use SharedPreferencesHandler to check if host exists in local storage
        val sharedPreferencesHandler = SharedPreferencesHandler(this)
        val userPreferences = sharedPreferencesHandler.getUserPreferences()
        val host = userPreferences.host
        if (host.isNotEmpty()) {
            // Redirect to MainActivity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        setContent {
            FahrenheitTheme {
                LoginScreen(loginHandler::handleLogin)
            }
        }
    }
}