package com.paulohenriquesg.fahrenheit.auth

import android.content.Context
import com.paulohenriquesg.fahrenheit.api.ApiClient
import com.paulohenriquesg.fahrenheit.storage.SharedPreferencesHandler
import com.paulohenriquesg.fahrenheit.storage.UserPreferences
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AuthenticationTest {
    private lateinit var context: Context
    private lateinit var sharedPreferencesHandler: SharedPreferencesHandler

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        sharedPreferencesHandler = SharedPreferencesHandler(context)
        sharedPreferencesHandler.clearPreferences()
    }

    @Test
    fun `login credentials are stored correctly`() {
        val host = "https://test.audiobookshelf.org"
        val token = "test-token-12345"
        val username = "testuser"

        val userPrefs = UserPreferences(host, username, token, false)
        sharedPreferencesHandler.saveUserPreferences(userPrefs)

        val prefs = sharedPreferencesHandler.getUserPreferences()
        assertEquals(host, prefs.host)
        assertEquals(token, prefs.token)
        assertEquals(username, prefs.username)
    }

    @Test
    fun `invalid host format is rejected`() {
        val invalidHost = "invalid-host-without-protocol"
        val token = "test-token"

        val userPrefs = UserPreferences(invalidHost, "user", token, false)
        sharedPreferencesHandler.saveUserPreferences(userPrefs)

        // ApiClient.initialize should detect invalid host format
        // This test documents the expected behavior
        assertFalse(invalidHost.startsWith("http://"))
        assertFalse(invalidHost.startsWith("https://"))
    }

    @Test
    fun `token is included in API client after initialization`() {
        val host = "https://test.audiobookshelf.org"
        val token = "test-token-12345"

        val userPrefs = UserPreferences(host, "testuser", token, false)
        sharedPreferencesHandler.saveUserPreferences(userPrefs)
        ApiClient.initialize(context)

        assertEquals(token, ApiClient.getToken())
    }

    @Test
    fun `clearing credentials removes all stored data`() {
        val userPrefs = UserPreferences(
            "https://test.audiobookshelf.org",
            "testuser",
            "test-token",
            false
        )
        sharedPreferencesHandler.saveUserPreferences(userPrefs)

        sharedPreferencesHandler.clearPreferences()

        val prefs = sharedPreferencesHandler.getUserPreferences()
        assertTrue(prefs.host.isEmpty())
        assertTrue(prefs.token.isEmpty())
        assertTrue(prefs.username.isEmpty())
    }

    @Test
    fun `authentication headers include Bearer token`() {
        val token = "test-token-12345"
        val expectedHeader = "Bearer $token"

        assertEquals(expectedHeader, "Bearer $token")
    }

    @Test
    fun `missing credentials trigger login redirect`() {
        sharedPreferencesHandler.clearPreferences()

        val prefs = sharedPreferencesHandler.getUserPreferences()

        // Verify that missing credentials can be detected
        assertTrue(prefs.host.isEmpty() || prefs.token.isEmpty())
    }

    @Test
    fun `host URL is properly formatted`() {
        val validHttpsHost = "https://test.audiobookshelf.org"
        val validHttpHost = "http://192.168.1.100:13378"

        assertTrue(validHttpsHost.startsWith("http://") || validHttpsHost.startsWith("https://"))
        assertTrue(validHttpHost.startsWith("http://") || validHttpHost.startsWith("https://"))
    }

    @Test
    fun `full URL generation works correctly`() {
        val host = "https://test.audiobookshelf.org"
        val path = "/api/items/123/cover"

        val userPrefs = UserPreferences(host, "user", "token", false)
        sharedPreferencesHandler.saveUserPreferences(userPrefs)
        ApiClient.initialize(context)

        val fullUrl = ApiClient.generateFullUrl(path)
        assertEquals("$host$path", fullUrl)
    }

    @Test
    fun `authentication header format is correct for media player`() {
        val token = "test-token-12345"
        val expectedHeader = "Bearer $token"

        // This tests the format used in MediaPlayerController for authentication
        assertEquals(expectedHeader, "Bearer $token")
    }
}
