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
}
