package com.paulohenriquesg.fahrenheit.storage

data class UserPreferences(
    val host: String,
    val username: String,
    val token: String,
    val darkTheme: Boolean,
    /**
     * Exchanged for a new access token once [token] expires. Null on installs that
     * signed in before refresh tokens existed, and on servers too old to issue one.
     */
    val refreshToken: String? = null,
    val isRowLayout: Boolean = true,
    val lastUpdateCheck: Long = 0L,
    val skipVersion: String? = null,
    val updateCheckEnabled: Boolean = true,
    val selectedLibraryId: String? = null
)