package com.paulohenriquesg.fahrenheit.main

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.lifecycle.Lifecycle
import com.paulohenriquesg.fahrenheit.R
import com.google.gson.Gson
import com.paulohenriquesg.fahrenheit.api.ApiClient
import com.paulohenriquesg.fahrenheit.api.LibraryItem
import com.paulohenriquesg.fahrenheit.api.Shelf
import com.paulohenriquesg.fahrenheit.storage.SharedPreferencesHandler
import com.paulohenriquesg.fahrenheit.storage.UserPreferences
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowToast

/**
 * Which failed loads of Home's shelves are said out loud (#197): the ones
 * asked for that leave Home empty. A reload in the background keeps what is
 * shown and says nothing - a TV back on Home over a network blip would toast
 * every time.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w960dp-h540dp")
class HomeShelvesFailureTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val server = MockWebServer()
    private val shelves = listOf(
        Shelf(id = "recently-added", label = "An Invented Shelf", labelStringKey = "", type = "book", bookEntities = listOf(book()))
    )
    private var answers = ArrayDeque<List<Shelf>?>()
    private var fetches = 0

    @Before
    fun setUp() {
        server.start()
        val context = compose.activity
        SharedPreferencesHandler(context).saveUserPreferences(
            UserPreferences(server.url("/").toString().trimEnd('/'), "user", "token", false)
        )
        ApiClient.initialize(context)
        server.enqueue(MockResponse().setBody("""{"libraries":[{"id":"lib","name":"Books","mediaType":"book","displayOrder":1}]}"""))
        ShadowToast.reset()
    }

    @After
    fun tearDown() {
        ApiClient.clearSession()
        server.shutdown()
    }

    private fun show(vararg answers: List<Shelf>?) {
        this.answers = ArrayDeque(answers.toList())
        compose.setContent {
            FahrenheitTheme {
                MainScreen(
                    fetchLibraryItems = { _, _ -> emptyList() },
                    // Answered on the main thread, as on a device: under test the
                    // effect would otherwise go on on the thread that read the libraries.
                    fetchPersonalizedView = {
                        withContext(Dispatchers.Main.immediate) { fetches++; this@HomeShelvesFailureTest.answers.removeFirstOrNull() }
                    },
                    listeningNews = MutableStateFlow(0)
                )
            }
        }
        // Generous bounds, met as soon as they hold: the first MainScreen and
        // client of a busy test JVM have taken over five seconds.
        compose.waitUntil(30_000) { server.requestCount >= 1 }
        compose.waitUntil(30_000) { fetches >= 1 }
        compose.waitForIdle()
    }

    /** Until the reload, past its coalescing window, has fetched. */
    private fun untilReloaded() {
        compose.waitUntil(30_000) { fetches >= 2 }
        compose.waitForIdle()
    }

    private fun book(): LibraryItem = Gson().fromJson(
        """{"id":"b1","ino":"1","libraryId":"lib","folderId":"f","path":"/p","relPath":"p",
            "isFile":false,"mtimeMs":0,"ctimeMs":0,"birthtimeMs":0,"addedAt":0,"updatedAt":0,
            "isMissing":false,"isInvalid":false,"mediaType":"book",
            "media":{"metadata":{"title":"An Invented Book","authorName":"A Writer"},"tags":[],
            "numTracks":1,"numAudioFiles":1,"numChapters":0,"duration":3480.0,"size":0}}""",
        LibraryItem::class.java
    )

    private fun failedText() = compose.activity.getString(R.string.home_shelves_failed)

    @Test
    fun `a first load that fails says so`() {
        show(null)

        assertEquals(failedText(), ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun `coming back to Home when the reload fails keeps the shelves and says nothing`() {
        show(shelves, null)
        assertEquals(1, compose.onAllNodesWithText("An Invented Shelf").fetchSemanticsNodes().size)

        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        untilReloaded()

        assertEquals("the reload was made", 2, fetches)
        assertNull(ShadowToast.getLatestToast())
        assertEquals(1, compose.onAllNodesWithText("An Invented Shelf").fetchSemanticsNodes().size)
    }

    @Test
    fun `choosing Home with nothing shown, a failure says so again`() {
        show(null, null)
        ShadowToast.reset()

        compose.onNodeWithTag(menuItemTestTag(MainView.HOME.menuItemId)).performSemanticsAction(SemanticsActions.OnClick)
        untilReloaded()

        assertEquals("the reload was made", 2, fetches)
        assertEquals(failedText(), ShadowToast.getTextOfLatestToast())
    }
}
