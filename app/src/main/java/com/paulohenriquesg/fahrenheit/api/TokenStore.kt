package com.paulohenriquesg.fahrenheit.api

/** The signed-in credentials, independent of how they are stored. */
data class StoredCredentials(
    val host: String = "",
    val username: String = "",
    val accessToken: String = "",
    val refreshToken: String? = null
) {
    companion object {
        val EMPTY = StoredCredentials()
    }
}

/**
 * Where the session lives.
 *
 * An interface so session logic can be tested without an Android framework:
 * SessionManager previously took SharedPreferencesHandler directly, which
 * forced every test touching a session onto Robolectric - roughly 34x slower
 * per test, and invisible to coverage without a JaCoCo workaround.
 */
interface TokenStore {
    fun read(): StoredCredentials

    /** Must not disturb unrelated user settings. */
    fun write(credentials: StoredCredentials)
}
