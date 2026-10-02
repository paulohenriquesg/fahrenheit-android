package com.paulohenriquesg.fahrenheit.player

import android.content.Context
import android.net.Uri
import androidx.media3.datasource.DataSourceUtil
import androidx.media3.datasource.DataSpec
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.paulohenriquesg.fahrenheit.api.ApiClient
import com.paulohenriquesg.fahrenheit.api.FakeTokenStore
import com.paulohenriquesg.fahrenheit.auth.AuthSession
import com.paulohenriquesg.fahrenheit.auth.SessionManager
import com.paulohenriquesg.fahrenheit.storage.SharedPreferencesHandler
import com.paulohenriquesg.fahrenheit.storage.UserPreferences
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Audio is fetched with the same credentials as every other request.
 *
 * An access token lasts an hour and is replaced on each refresh, so a long
 * book outlives the token it started with: file three must carry whatever
 * token is current when it is opened, not the one the queue was built with.
 */
@RunWith(AndroidJUnit4::class)
class AudioHttpTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
        ApiClient.clearSession()
    }

    private fun sessionWith(access: String) = SessionManager(FakeTokenStore()).apply {
        persist(host = server.url("/").toString().trimEnd('/'), session = AuthSession(access, "valid-refresh", "listener"))
    }

    private fun read(factory: androidx.media3.datasource.DataSource.Factory, path: String): String {
        val source = factory.createDataSource()
        source.open(DataSpec(Uri.parse(server.url(path).toString())))
        return try { String(DataSourceUtil.readToEnd(source)) } finally { source.close() }
    }

    // Review Focus 4.
    @Test
    fun `an expired token is refreshed for audio too`() {
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(200).setBody("audio-bytes"))
        val client = ApiClient.buildAuthenticatedClient(sessionWith("expired-access")) {
            AuthSession("fresh-access", "rotated-refresh", "listener")
        }

        val body = read(AudioHttp.dataSourceFactory { client }, "/api/items/b1/file/2")

        assertEquals("audio-bytes", body)
        assertEquals("Bearer expired-access", server.takeRequest().getHeader("Authorization"))
        assertEquals("Bearer fresh-access", server.takeRequest().getHeader("Authorization"))
    }

    // Review Focus 4: signed out and in as someone else between two files.
    @Test
    fun `each file uses the client current when it is opened`() {
        server.enqueue(MockResponse().setBody("one"))
        server.enqueue(MockResponse().setBody("two"))
        var current: OkHttpClient = ApiClient.buildAuthenticatedClient(sessionWith("first-user")) { error("no refresh") }
        val factory = AudioHttp.dataSourceFactory { current }

        read(factory, "/file/1")
        current = ApiClient.buildAuthenticatedClient(sessionWith("second-user")) { error("no refresh") }
        read(factory, "/file/2")

        assertEquals("Bearer first-user", server.takeRequest().getHeader("Authorization"))
        assertEquals("Bearer second-user", server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun `the app hands out an audio client while signed in, and none after`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        SharedPreferencesHandler(context).saveUserPreferences(UserPreferences("https://abs.test", "listener", "token", false))
        ApiClient.initialize(context)
        assertNotNull(ApiClient.audioHttpClient())

        ApiClient.clearSession()

        assertNull(ApiClient.audioHttpClient())
    }
}
