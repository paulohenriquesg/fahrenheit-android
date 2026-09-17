package com.paulohenriquesg.fahrenheit.storage

import android.content.Context
import com.paulohenriquesg.fahrenheit.api.StoredCredentials
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Robolectric on purpose: this is the one place that must prove real encrypted
 * SharedPreferences round-trip correctly. The in-memory FakeTokenStore used by
 * the session tests cannot tell us anything about that.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SharedPreferencesTokenStoreTest {
    private lateinit var preferences: SharedPreferencesHandler
    private lateinit var store: SharedPreferencesTokenStore

    @Before
    fun setup() {
        val context: Context = RuntimeEnvironment.getApplication()
        preferences = SharedPreferencesHandler(context)
        preferences.clearPreferences()
        store = SharedPreferencesTokenStore(preferences)
    }

    @Test
    fun `credentials survive a real round trip`() {
        store.write(
            StoredCredentials("http://abs.local", "testuser", "access", "refresh")
        )

        val read = store.read()
        assertEquals("http://abs.local", read.host)
        assertEquals("testuser", read.username)
        assertEquals("access", read.accessToken)
        assertEquals("refresh", read.refreshToken)
    }

    @Test
    fun `writing credentials leaves unrelated settings alone`() {
        // Signing in again must not reset the user's library or display choices,
        // which share the same preferences object.
        preferences.saveUserPreferences(
            UserPreferences(
                host = "http://old.local",
                username = "olduser",
                token = "old",
                darkTheme = true,
                isRowLayout = false,
                selectedLibraryId = "library-456"
            )
        )

        store.write(StoredCredentials("http://abs.local", "testuser", "new-access", "new-refresh"))

        val prefs = preferences.getUserPreferences()
        assertEquals("library-456", prefs.selectedLibraryId)
        assertTrue(prefs.darkTheme)
        assertFalse(prefs.isRowLayout)
        assertEquals("new-access", prefs.token)
    }

    @Test
    fun `an install predating refresh tokens reads back null, not empty string`() {
        preferences.saveUserPreferences(
            UserPreferences(
                host = "http://abs.local",
                username = "testuser",
                token = "legacy-non-expiring",
                darkTheme = false
            )
        )

        assertNull(store.read().refreshToken)
    }
}
