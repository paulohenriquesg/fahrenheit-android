package com.paulohenriquesg.fahrenheit.auth

import android.content.Context
import com.paulohenriquesg.fahrenheit.storage.SharedPreferencesHandler
import com.paulohenriquesg.fahrenheit.storage.UserPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * The access token expires after an hour, so the refresh token has to outlive the
 * process the same way the access token does.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class RefreshTokenStorageTest {
    private lateinit var context: Context
    private lateinit var handler: SharedPreferencesHandler

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        handler = SharedPreferencesHandler(context)
        handler.clearPreferences()
    }

    @Test
    fun `refresh token survives a round trip`() {
        handler.saveUserPreferences(
            UserPreferences(
                host = "http://abs.local",
                username = "testuser",
                token = "access-token",
                refreshToken = "refresh-token",
                darkTheme = false
            )
        )

        val prefs = handler.getUserPreferences()

        assertEquals("access-token", prefs.token)
        assertEquals("refresh-token", prefs.refreshToken)
    }

    @Test
    fun `an install predating refresh tokens reads back null rather than empty string`() {
        // Upgrading users have a stored `token` but no `refreshToken`. A "" here would
        // later be sent as a valid-looking x-refresh-token header and 401.
        handler.saveUserPreferences(
            UserPreferences(
                host = "http://abs.local",
                username = "testuser",
                token = "legacy-non-expiring",
                darkTheme = false
            )
        )

        assertNull(handler.getUserPreferences().refreshToken)
    }
}
