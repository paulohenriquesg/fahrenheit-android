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
    /** Version the user pushed away, and when: it stays quiet for a day. */
    val updateSnoozeVersionCode: Int? = null,
    val updateSnoozeAt: Long? = null,
    /** Version handed to the system installer, to check it landed next launch. */
    val pendingInstallVersionCode: Int? = null,
    val updateCheckEnabled: Boolean = true,
    val selectedLibraryId: String? = null
)