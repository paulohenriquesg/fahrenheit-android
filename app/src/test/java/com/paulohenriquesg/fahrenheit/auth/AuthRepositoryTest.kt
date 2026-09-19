package com.paulohenriquesg.fahrenheit.auth

import com.paulohenriquesg.fahrenheit.TestFixtures
import com.paulohenriquesg.fahrenheit.api.AuthApi
import com.paulohenriquesg.fahrenheit.api.AuthRepository
import com.paulohenriquesg.fahrenheit.api.LoginRequest
import com.paulohenriquesg.fahrenheit.api.LoginResponse
import com.paulohenriquesg.fahrenheit.api.ServerStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Audiobookshelf >= 2.26 issues a short-lived access token (1h) plus a long-lived
 * refresh token (30d). The legacy `user.token` is a non-expiring token the server
 * still emits for backwards compatibility but has flagged for removal.
 *
 * These tests pin the client to the modern fields while staying compatible with
 * servers old enough to only send `user.token`.
 */
class AuthRepositoryTest {

    /** Records what the repository sent, and replays a canned response. */
    private class FakeAuthApi(
        private val loginResponse: LoginResponse? = null,
        private val refreshResponse: LoginResponse? = null
    ) : AuthApi {
        var returnTokensHeader: String? = null
        var refreshTokenHeader: String? = null
        var loginRequest: LoginRequest? = null

        var statusResponse: ServerStatus = ServerStatus(authMethods = listOf("local"))
        var authorizeResponse: LoginResponse? = null
        var authorizeHeader: String? = null

        override suspend fun authorize(bearer: String): LoginResponse {
            authorizeHeader = bearer
            return authorizeResponse ?: error("no authorize response configured")
        }

        override suspend fun status(): ServerStatus = statusResponse

        override suspend fun login(returnTokens: String, request: LoginRequest): LoginResponse {
            returnTokensHeader = returnTokens
            loginRequest = request
            return loginResponse ?: error("no login response configured")
        }

        override suspend fun refresh(refreshToken: String): LoginResponse {
            refreshTokenHeader = refreshToken
            return refreshResponse ?: error("no refresh response configured")
        }
    }

    @Test
    fun `login prefers the short-lived access token over the legacy token`() = runBlocking {
        val api = FakeAuthApi(
            loginResponse = TestFixtures.createMockLoginResponse(
                token = "legacy-non-expiring",
                accessToken = "fresh-access-token",
                refreshToken = "long-lived-refresh"
            )
        )

        val session = AuthRepository(api).login("testuser", "hunter2", "http://abs.local")

        assertEquals("fresh-access-token", session.accessToken)
        assertEquals("long-lived-refresh", session.refreshToken)
    }

    @Test
    fun `login asks the server to return the refresh token in the body`() = runBlocking {
        // Without x-return-tokens the server only sets a cookie, which a Retrofit
        // client without a cookie jar silently drops - leaving no way to refresh.
        val api = FakeAuthApi(
            loginResponse = TestFixtures.createMockLoginResponse(
                accessToken = "a",
                refreshToken = "r"
            )
        )

        AuthRepository(api).login("testuser", "hunter2", "http://abs.local")

        assertEquals("true", api.returnTokensHeader)
    }

    @Test
    fun `login falls back to the legacy token when the server sends no access token`() =
        runBlocking {
            // Audiobookshelf older than 2.26 has no accessToken field at all.
            val api = FakeAuthApi(
                loginResponse = TestFixtures.createMockLoginResponse(
                    token = "legacy-non-expiring",
                    accessToken = null,
                    refreshToken = null
                )
            )

            val session = AuthRepository(api).login("testuser", "hunter2", "http://abs.local")

            assertEquals("legacy-non-expiring", session.accessToken)
            assertNull(session.refreshToken)
        }

    @Test
    fun `refresh exchanges a refresh token for a new access token`() = runBlocking {
        val api = FakeAuthApi(
            refreshResponse = TestFixtures.createMockLoginResponse(
                accessToken = "rotated-access-token",
                refreshToken = "rotated-refresh-token"
            )
        )

        val session = AuthRepository(api).refresh("long-lived-refresh")

        assertEquals("long-lived-refresh", api.refreshTokenHeader)
        assertEquals("rotated-access-token", session.accessToken)
        assertEquals("rotated-refresh-token", session.refreshToken)
    }

    @Test
    fun `an API key session stores the key itself, not the token the server echoes back`() =
        runBlocking {
            // /api/authorize returns the login payload WITHOUT accessToken but WITH
            // the legacy non-expiring user.token. Reusing toAuthSession() would pick
            // that legacy token and silently put API-key users back on the
            // deprecated field PR #3 moved away from.
            val api = FakeAuthApi()
            api.authorizeResponse = TestFixtures.createMockLoginResponse(
                token = "legacy-echoed-back",
                accessToken = null,
                refreshToken = null
            )

            val session = AuthRepository(api).signInWithApiKey("my-api-key")

            assertEquals("my-api-key", session.accessToken)
        }

    @Test
    fun `the API key is sent as a bearer token`() = runBlocking {
        val api = FakeAuthApi()
        api.authorizeResponse = TestFixtures.createMockLoginResponse()

        AuthRepository(api).signInWithApiKey("my-api-key")

        assertEquals("Bearer my-api-key", api.authorizeHeader)
    }

    @Test
    fun `an API key session has nothing to refresh with`() = runBlocking {
        // API keys are long-lived and do not rotate; a refresh token here would
        // make the 401 authenticator try to refresh something that cannot be.
        val api = FakeAuthApi()
        api.authorizeResponse = TestFixtures.createMockLoginResponse(refreshToken = "should-be-ignored")

        assertNull(AuthRepository(api).signInWithApiKey("my-api-key").refreshToken)
    }

    @Test
    fun `the API key session takes its username from the server`() = runBlocking {
        val api = FakeAuthApi()
        api.authorizeResponse = TestFixtures.createMockLoginResponse()

        assertEquals("testuser", AuthRepository(api).signInWithApiKey("my-api-key").username)
    }
}
