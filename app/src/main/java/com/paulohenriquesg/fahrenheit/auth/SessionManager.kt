package com.paulohenriquesg.fahrenheit.auth

/**
 * Single owner of the signed-in token state.
 *
 * Both the login flow and the 401 refresh path write through here, so the
 * stored access token and refresh token cannot drift apart.
 *
 * Takes a [TokenStore] rather than SharedPreferences directly, so session logic
 * can be exercised without an Android framework.
 */
class SessionManager(private val store: TokenStore) {

    fun persist(host: String, session: AuthSession) {
        store.write(
            StoredCredentials(
                host = host,
                username = session.username,
                accessToken = session.accessToken,
                refreshToken = session.refreshToken
            )
        )
    }

    /** Null when the server issued no refresh token, i.e. nothing to retry with. */
    fun refreshToken(): String? = store.read().refreshToken

    fun accessToken(): String? = store.read().accessToken.takeIf { it.isNotEmpty() }

    fun host(): String = store.read().host
}
