package com.paulohenriquesg.fahrenheit.storage

data class UserPreferences(
    val host: String,
    val username: String,
    val token: String,
    val darkTheme: Boolean,
    val isRowLayout: Boolean = true,
    val lastUpdateCheck: Long = 0L,
    val skipVersion: String? = null,
    val updateCheckEnabled: Boolean = true,
    val selectedLibraryId: String? = null
)