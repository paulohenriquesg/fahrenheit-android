package com.paulohenriquesg.fahrenheit.api

import android.app.Application
import com.paulohenriquesg.fahrenheit.storage.SharedPreferencesHandler
import com.paulohenriquesg.fahrenheit.storage.UserPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * ApiClient.initialize used to start LoginActivity itself when credentials were
 * missing or malformed. Two problems with that:
 *
 *  - FahrenheitApplication calls it from Application.onCreate, so the app could
 *    launch an Activity before the launcher Activity exists, racing it.
 *  - A network client deciding what the user sees makes the whole class
 *    untestable, which is why none of this was covered.
 *
 * It now reports what it found and lets the caller decide.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ApiClientInitializeTest {
    private lateinit var app: Application
    private lateinit var prefs: SharedPreferencesHandler

    @Before
    fun setup() {
        app = RuntimeEnvironment.getApplication()
        prefs = SharedPreferencesHandler(app)
        prefs.clearPreferences()
        shadowOf(app).clearNextStartedActivities()
    }

    private fun store(host: String, token: String) {
        prefs.saveUserPreferences(
            UserPreferences(host = host, username = "testuser", token = token, darkTheme = false)
        )
    }

    @Test
    fun `usable credentials yield a ready session and a working client`() {
        store("http://abs.local:13378", "a-token")

        val state = ApiClient.initialize(app)

        assertEquals(SessionState.Ready, state)
        assertNotNull(ApiClient.getApiService())
    }

    @Test
    fun `a missing token needs login and does not navigate`() {
        store("http://abs.local:13378", "")

        val state = ApiClient.initialize(app)

        assertEquals(SessionState.NeedsLogin, state)
        assertNull(
            "initialize must not start an Activity - the caller decides",
            shadowOf(app).nextStartedActivity
        )
    }

    @Test
    fun `a host with no scheme needs login`() {
        store("abs.local:13378", "a-token")

        assertEquals(SessionState.NeedsLogin, ApiClient.initialize(app))
        assertNull(shadowOf(app).nextStartedActivity)
    }

    @Test
    fun `an empty host needs login`() {
        store("", "a-token")

        assertEquals(SessionState.NeedsLogin, ApiClient.initialize(app))
    }

    @Test
    fun `needing login clears the unusable credentials`() {
        store("not-a-url", "a-token")

        ApiClient.initialize(app)

        assertEquals("", prefs.getUserPreferences().host)
        assertEquals("", prefs.getUserPreferences().token)
    }

    @Test
    fun `an https host is accepted`() {
        store("https://abs.example.com", "a-token")

        assertEquals(SessionState.Ready, ApiClient.initialize(app))
    }
}
