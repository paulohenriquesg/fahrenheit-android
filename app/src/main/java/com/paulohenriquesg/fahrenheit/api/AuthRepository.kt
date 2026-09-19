package com.paulohenriquesg.fahrenheit.api

/** The credentials a signed-in session needs, independent of how the server supplied them. */
data class AuthSession(
    val accessToken: String,
    val refreshToken: String?,
    val username: String
)

/**
 * Wraps [AuthApi] so callers deal in [AuthSession] rather than in the server's
 * backwards-compatibility shape.
 *
 * Audiobookshelf 2.26 replaced the non-expiring `user.token` with a 1 hour
 * `accessToken` plus a 30 day `refreshToken`. The old field is still emitted, so
 * prefer the access token and fall back to the legacy one for older servers.
 */
class AuthRepository(private val api: AuthApi) {

    suspend fun login(username: String, password: String, host: String): AuthSession =
        api.login(RETURN_TOKENS_IN_BODY, LoginRequest(username, password, host)).toAuthSession()

    /**
     * Signs in with an API key created in the Audiobookshelf web UI, for users
     * whose account has no password.
     *
     * Stores the key itself rather than calling toAuthSession(): /api/authorize
     * returns no accessToken but does echo the legacy non-expiring user.token,
     * which toAuthSession() would pick - quietly putting API-key users back on
     * the deprecated field.
     *
     * No refresh token: API keys are long-lived and do not rotate, so there is
     * nothing for the 401 authenticator to exchange.
     */
    suspend fun signInWithApiKey(apiKey: String): AuthSession {
        val response = api.authorize("Bearer $apiKey")
        return AuthSession(
            accessToken = apiKey,
            refreshToken = null,
            username = response.user.username
        )
    }

    suspend fun refresh(refreshToken: String): AuthSession =
        api.refresh(refreshToken).toAuthSession()


    private companion object {
        /** Without this the refresh token is only set as a cookie, which we never read. */
        const val RETURN_TOKENS_IN_BODY = "true"
    }
}

/**
 * Prefer the short-lived access token, falling back to the legacy non-expiring one
 * for servers older than 2.26. Shared by the login and refresh paths so they cannot
 * disagree about which token to use.
 */
fun LoginResponse.toAuthSession() = AuthSession(
    accessToken = user.accessToken ?: user.token,
    refreshToken = user.refreshToken,
    username = user.username
)
