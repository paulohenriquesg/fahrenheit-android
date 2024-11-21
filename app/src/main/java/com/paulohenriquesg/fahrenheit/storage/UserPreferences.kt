package com.paulohenriquesg.fahrenheit.storage

data class UserPreferences(
    val host: String,
    val username: String,
    val token: String,
    val darkTheme: Boolean
)