package com.paulohenriquesg.fahrenheit.main

import com.paulohenriquesg.fahrenheit.api.ApiClient
import com.paulohenriquesg.fahrenheit.storage.SharedPreferencesHandler
import com.paulohenriquesg.fahrenheit.storage.UserPreferences
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.shadows.ShadowToast

/** Home's shelves: "could not read them" is not "there are none" (#197). */
@RunWith(RobolectricTestRunner::class)
class MainHandlerTest {

    private val context = RuntimeEnvironment.getApplication()
    private val server = MockWebServer()

    /** What the personalized shelves answer; anything else - Favourites, say - is not there. */
    private var shelves = MockResponse().setResponseCode(500)

    @Before
    fun setUp() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse =
                if (request.path.orEmpty().contains("/personalized")) shelves else MockResponse().setResponseCode(404)
        }
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

        assertNull(runBlocking { MainHandler(context).fetchPersonalizedView("lib") })
    }

    // Coordinator: whether a failure is worth a toast is the caller's call (#197).
    @Test
    fun `shelves that cannot be read raise no toast`() {

        runBlocking { MainHandler(context).fetchPersonalizedView("lib") }

        assertNull(ShadowToast.getLatestToast())
    }

    @Test
    fun `signed out, there are no shelves to read`() {
        ApiClient.clearSession()

        assertNull(runBlocking { MainHandler(context).fetchPersonalizedView("lib") })
    }

    @Test
    fun `a library with nothing to show has no shelves`() {
        shelves = MockResponse().setBody("[]")

        assertEquals(emptyList<Any>(), runBlocking { MainHandler(context).fetchPersonalizedView("lib") })
    }
}
