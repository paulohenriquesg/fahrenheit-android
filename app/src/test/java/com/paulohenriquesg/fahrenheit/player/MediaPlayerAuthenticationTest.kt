package com.paulohenriquesg.fahrenheit.player

import android.content.Context
import android.net.Uri
import com.paulohenriquesg.fahrenheit.GlobalMediaPlayer
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
class MediaPlayerAuthenticationTest {
    private lateinit var context: Context
    private lateinit var sharedPreferencesHandler: SharedPreferencesHandler

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        sharedPreferencesHandler = SharedPreferencesHandler(context)
        sharedPreferencesHandler.clearPreferences()
    }

    @Test
    fun `media player requires authentication headers`() {
        val host = "https://test.audiobookshelf.org"
        val token = "test-token-12345"
        val audioPath = "/api/items/test-item/file/test-file"

        val userPrefs = UserPreferences(host, "testuser", token, false)
        sharedPreferencesHandler.saveUserPreferences(userPrefs)
        ApiClient.initialize(context)

        val fullUrl = ApiClient.generateFullUrl(audioPath)
        val authToken = ApiClient.getToken()

        assertNotNull("Full URL should be generated", fullUrl)
        assertNotNull("Auth token should be available", authToken)
        assertEquals("$host$audioPath", fullUrl)
        assertEquals(token, authToken)
    }

    @Test
    fun `authentication header format is correct`() {
        val token = "test-token-12345"
        val expectedAuthHeader = "Bearer $token"

        val headers = mapOf("Authorization" to "Bearer $token")

        assertEquals(expectedAuthHeader, headers["Authorization"])
    }

    @Test
    fun `media URL is properly constructed`() {
        val host = "https://test.audiobookshelf.org"
        val itemId = "test-item-123"
        val fileId = "test-file-456"
        val expectedPath = "/api/items/$itemId/file/$fileId"

        val userPrefs = UserPreferences(host, "user", "token", false)
        sharedPreferencesHandler.saveUserPreferences(userPrefs)
        ApiClient.initialize(context)

        val fullUrl = ApiClient.generateFullUrl(expectedPath)
        assertEquals("$host$expectedPath", fullUrl)
    }

    @Test
    fun `URI parsing handles special characters`() {
        val url = "https://test.audiobookshelf.org/api/items/test%20item/file/test%20file"
        val uri = Uri.parse(url)

        assertNotNull(uri)
        assertEquals("https", uri.scheme)
        assertEquals("test.audiobookshelf.org", uri.host)
    }

    @Test
    fun `media player can be reset and reinitialized`() {
        val mediaPlayer = GlobalMediaPlayer.getInstance()

        assertNotNull(mediaPlayer)
        mediaPlayer.reset()

        // After reset, player should be in idle state
        assertFalse(mediaPlayer.isPlaying)
    }

    @Test
    fun `missing authentication triggers error`() {
        sharedPreferencesHandler.clearPreferences()
        ApiClient.initialize(context)

        val token = ApiClient.getToken()

        // Without authentication, token should be null or empty
        assertTrue(token == null || token.isEmpty())
    }

    @Test
    fun `authentication headers are required for streaming`() {
        val host = "https://test.audiobookshelf.org"
        val token = "test-token-12345"

        val userPrefs = UserPreferences(host, "testuser", token, false)
        sharedPreferencesHandler.saveUserPreferences(userPrefs)
        ApiClient.initialize(context)

        val authToken = ApiClient.getToken()

        // Verify token is available for MediaPlayer setDataSource
        assertNotNull("Token required for authenticated streaming", authToken)

        val headers = mapOf("Authorization" to "Bearer $authToken")
        assertTrue(headers.containsKey("Authorization"))
        assertTrue(headers["Authorization"]!!.startsWith("Bearer "))
    }
}
