package com.paulohenriquesg.fahrenheit.main

import com.paulohenriquesg.fahrenheit.api.ApiClient
import com.paulohenriquesg.fahrenheit.storage.SharedPreferencesHandler
import com.paulohenriquesg.fahrenheit.storage.UserPreferences
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** Home's shelves: "could not read them" is not "there are none" (#197). */
@RunWith(RobolectricTestRunner::class)
class MainHandlerTest {

    private val context = RuntimeEnvironment.getApplication()
    private val server = MockWebServer()

    @Before
    fun setUp() {
        server.start()
        SharedPreferencesHandler(context).saveUserPreferences(
            UserPreferences(server.url("/").toString().trimEnd('/'), "user", "token", false)
        )
        ApiClient.initialize(context)
    }

    @After
    fun tearDown() {
        ApiClient.clearSession()
        server.shutdown()
    }

    @Test
    fun `shelves that cannot be read are null`() {
        server.enqueue(MockResponse().setResponseCode(500))

        assertNull(runBlocking { MainHandler(context).fetchPersonalizedView("lib") })
    }

    @Test
    fun `signed out, there are no shelves to read`() {
        ApiClient.clearSession()

        assertNull(runBlocking { MainHandler(context).fetchPersonalizedView("lib") })
    }

    @Test
    fun `a library with nothing to show has no shelves`() {
        server.enqueue(MockResponse().setBody("[]"))

        assertEquals(emptyList<Any>(), runBlocking { MainHandler(context).fetchPersonalizedView("lib") })
    }
}
