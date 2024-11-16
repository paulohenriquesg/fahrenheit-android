package com.paulohenriquesg.fahrenheit.api

data class LoginRequest(
    val username: String,
    val password: String,
    val host: String
)