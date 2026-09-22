package com.paulohenriquesg.fahrenheit.api

import okhttp3.mockwebserver.MockResponse
import com.paulohenriquesg.fahrenheit.auth.SessionManager
import com.paulohenriquesg.fahrenheit.auth.AuthSession
import okhttp3.mockwebserver.MockWebServer
import okhttp3.Request
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Exercises the whole OkHttp stack - auth interceptor, authenticator, retry -
 * against a real server socket.
 *
 * TokenRefreshAuthenticatorTest covers the decision in isolation with a
 * hand-built 401. This covers the part that only breaks when the pieces are
 * wired together: that the retry actually carries the new token, and that every
 * later request does too.
 */
class TokenRefreshIntegrationTest {
    private lateinit var server: MockWebServer
    private lateinit var sessionManager: SessionManager

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()
        sessionManager = SessionManager(FakeTokenStore())
        sessionManager.persist(
            host = server.url("/").toString().trimEnd('/'),
            session = AuthSession("expired-access", "valid-refresh", "testuser")
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun clientRefreshingTo(newSession: AuthSession?) =
        ApiClient.buildAuthenticatedClient(sessionManager) {
            newSession ?: throw java.io.IOException("refresh rejected")
        }

    @Test
    fun `an expired token is refreshed and the request is retried with the new one`() {
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"ok":true}"""))

        val client = clientRefreshingTo(AuthSession("fresh-access", "rotated-refresh", "testuser"))
        val response = client
            .newCall(Request.Builder().url(server.url("/api/libraries")).build())
            .execute()

        assertEquals(200, response.code)
        response.close()

        val first = server.takeRequest()
        val retry = server.takeRequest()
        assertEquals("Bearer expired-access", first.getHeader("Authorization"))
        assertEquals("Bearer fresh-access", retry.getHeader("Authorization"))
    }

    @Test
    fun `the refreshed token is used for subsequent requests, not just the retry`() {
        // The bug this guards: capturing the token when the client is built means
        // every later call keeps sending the dead one and 401s forever.
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))

        val client = clientRefreshingTo(AuthSession("fresh-access", "rotated-refresh", "testuser"))
        client.newCall(Request.Builder().url(server.url("/api/libraries")).build()).execute().close()
        client.newCall(Request.Builder().url(server.url("/api/me")).build()).execute().close()

        server.takeRequest() // initial 401
        server.takeRequest() // retry
        val later = server.takeRequest()
        assertEquals("Bearer fresh-access", later.getHeader("Authorization"))
    }

    @Test
    fun `a rejected refresh surfaces the 401 instead of retrying forever`() {
        repeat(4) { server.enqueue(MockResponse().setResponseCode(401)) }

        val client = clientRefreshingTo(null)
        val response = client
            .newCall(Request.Builder().url(server.url("/api/libraries")).build())
            .execute()

        assertEquals(401, response.code)
        response.close()
        assertEquals("must not retry when the refresh failed", 1, server.requestCount)
    }

    @Test
    fun `a session with no refresh token does not attempt a refresh`() {
        sessionManager.persist(
            host = server.url("/").toString().trimEnd('/'),
            session = AuthSession("legacy-non-expiring", null, "testuser")
        )
        server.enqueue(MockResponse().setResponseCode(401))

        val client = ApiClient.buildAuthenticatedClient(sessionManager) {
            throw AssertionError("must not refresh without a refresh token")
        }
        val response = client
            .newCall(Request.Builder().url(server.url("/api/libraries")).build())
            .execute()

        assertEquals(401, response.code)
        response.close()
        assertNull(sessionManager.refreshToken())
    }
}
