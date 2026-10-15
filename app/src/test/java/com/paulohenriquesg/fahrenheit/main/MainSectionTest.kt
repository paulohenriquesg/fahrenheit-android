package com.paulohenriquesg.fahrenheit.main

import android.content.Context
import android.content.Intent
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import com.paulohenriquesg.fahrenheit.api.ApiClient
import com.paulohenriquesg.fahrenheit.storage.SharedPreferencesHandler
import com.paulohenriquesg.fahrenheit.storage.UserPreferences
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * A section chosen from the side menu on another screen - the author's page
 * (#230) - opens the main screen on that section, as the rail here would.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w960dp-h540dp")
class MainSectionTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val server = MockWebServer()

    @Before
    fun setUp() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when {
                request.path.orEmpty() == "/api/libraries" ->
                    MockResponse().setBody("""{"libraries":[{"id":"lib","name":"Books","mediaType":"book","displayOrder":1}]}""")
                else -> MockResponse().setResponseCode(404)
            }
        }
        server.start()
        SharedPreferencesHandler(compose.activity).saveUserPreferences(
            UserPreferences(server.url("/").toString().trimEnd('/'), "user", "token", false)
        )
        ApiClient.initialize(compose.activity)
    }

    @After
    fun tearDown() {
        shadowOf(Looper.getMainLooper()).idle()
        ApiClient.clearSession()
        server.shutdown()
    }

    @Test
    fun `the intent names the section and brings back the main screen there is`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = MainActivity.sectionIntent(context, "series")

        assertEquals(MainActivity::class.java.name, intent.component?.className)
        assertEquals("series", MainActivity.sectionOf(intent))
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_CLEAR_TOP != 0)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_SINGLE_TOP != 0)
        assertNull(MainActivity.sectionOf(Intent(context, MainActivity::class.java)))
    }

    @Test
    fun `the main screen opens on the section asked for, once`() {
        var asked by mutableStateOf<String?>("narrators")
        var opened = 0
        compose.setContent {
            FahrenheitTheme {
                MainScreen(
                    fetchLibraryItems = { _, _ -> emptyList() },
                    fetchPersonalizedView = { emptyList() },
                    openSection = asked,
                    onSectionOpened = { opened++; asked = null }
                )
            }
        }
        // The section is in the menu once the library, a library of books, is read.
        compose.waitUntil(30_000) {
            compose.onAllNodesWithTag(menuItemTestTag("narrators")).fetchSemanticsNodes().isNotEmpty()
        }
        compose.waitForIdle()

        compose.onNodeWithTag(menuItemTestTag("narrators")).assertIsSelected()
        assertEquals(1, opened)
    }
}
