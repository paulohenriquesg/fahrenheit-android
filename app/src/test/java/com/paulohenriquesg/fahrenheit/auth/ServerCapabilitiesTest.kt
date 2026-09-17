package com.paulohenriquesg.fahrenheit.auth

import com.google.gson.GsonBuilder
import com.paulohenriquesg.fahrenheit.api.ServerStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `GET /status` is unauthenticated and reports which sign-in methods a server
 * offers. It is the supported way for a client to discover whether passwordless
 * sign-in is available, which #2 needs: the app currently assumes a password
 * always exists and rejects a blank one before any request is made.
 *
 * Payloads here are verbatim from a real 2.36.0 server, plus the shapes older
 * or differently configured servers produce. Issue #1 was a field whose real
 * type did not match the model, so these lean on tolerance, not optimism.
 */
class ServerCapabilitiesTest {
    private val gson = GsonBuilder().create()

    private fun parse(json: String) = gson.fromJson(json, ServerStatus::class.java)

    @Test
    fun `a password-only server offers local sign-in and not openid`() {
        // Verbatim from a live 2.36.0 instance.
        val status = parse(
            """
            {
                "app": "audiobookshelf",
                "serverVersion": "2.36.0",
                "isInit": true,
                "language": "en-us",
                "authMethods": ["local"],
                "authFormData": { "authLoginCustomMessage": "" }
            }
            """.trimIndent()
        )

        assertEquals("2.36.0", status.serverVersion)
        assertTrue(status.supportsLocal)
        assertFalse(status.supportsOpenId)
        assertTrue(status.requiresPassword)
    }

    @Test
    fun `a server with SSO configured offers openid`() {
        val status = parse(
            """
            {
                "app": "audiobookshelf",
                "serverVersion": "2.36.0",
                "isInit": true,
                "authMethods": ["local", "openid"],
                "authFormData": {
                    "authLoginCustomMessage": "",
                    "authOpenIDButtonText": "Sign in with SSO",
                    "authOpenIDAutoLaunch": false
                }
            }
            """.trimIndent()
        )

        assertTrue(status.supportsOpenId)
        assertEquals("Sign in with SSO", status.openIdButtonText)
    }

    @Test
    fun `an openid-only server must not demand a password`() {
        // The #2 case: the reporter cannot sign in because they have no password.
        val status = parse(
            """
            { "app": "audiobookshelf", "serverVersion": "2.36.0", "authMethods": ["openid"] }
            """.trimIndent()
        )

        assertFalse(status.supportsLocal)
        assertTrue(status.supportsOpenId)
        assertFalse("a password field would block this user entirely", status.requiresPassword)
    }

    @Test
    fun `a server too old to report authMethods is treated as password-only`() {
        // Absent field, not an empty list: assume the pre-existing behaviour
        // rather than locking the user out of a form that does work.
        val status = parse("""{ "app": "audiobookshelf", "serverVersion": "2.2.0" }""")

        assertTrue(status.supportsLocal)
        assertFalse(status.supportsOpenId)
        assertTrue(status.requiresPassword)
    }

    @Test
    fun `an unknown auth method does not disable the ones we understand`() {
        val status = parse(
            """{ "app": "audiobookshelf", "authMethods": ["local", "some-future-method"] }"""
        )

        assertTrue(status.supportsLocal)
        assertFalse(status.supportsOpenId)
    }
}
