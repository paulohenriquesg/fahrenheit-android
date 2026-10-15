package com.paulohenriquesg.fahrenheit.author

import android.app.Activity
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.media3.session.MediaSessionService
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.paulohenriquesg.fahrenheit.api.ApiClient
import com.paulohenriquesg.fahrenheit.main.MainActivity
import com.paulohenriquesg.fahrenheit.main.menuItemTestTag
import com.paulohenriquesg.fahrenheit.player.PlaybackService
import com.paulohenriquesg.fahrenheit.player.PlayerPreview
import com.paulohenriquesg.fahrenheit.storage.SharedPreferencesHandler
import com.paulohenriquesg.fahrenheit.storage.UserPreferences
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The author's and narrator's page as its own screen (#230): the side menu
 * is on it, its section highlighted; a section chosen there opens the main
 * screen on it, and Back leaves the page as it leaves the book page.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w960dp-h540dp")
class PersonActivityTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val server = MockWebServer()
    private var scenario: ActivityScenario<AuthorDetailActivity>? = null
    // The rail's Now playing connects to it, as on a device.
    private val service = Robolectric.buildService(PlaybackService::class.java).create()
    private val asked = mutableListOf<String>()

    private fun book(id: String, title: String) = """{"id":"$id","ino":"1","libraryId":"lib","folderId":"f","path":"/p","relPath":"p",
        "isFile":false,"mtimeMs":0,"ctimeMs":0,"birthtimeMs":0,"addedAt":0,"updatedAt":0,
        "isMissing":false,"isInvalid":false,"mediaType":"book","numFiles":1,"size":0,
        "media":{"metadata":{"title":"$title","authorName":"An Author"},"tags":[],"numTracks":1,"numAudioFiles":1,
        "numChapters":0,"duration":3600.0,"size":0}}"""

    @Before
    fun setUp() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path.orEmpty()
                synchronized(asked) { asked += path }
                return when {
                    path.startsWith("/api/authors/a1") -> MockResponse().setBody(
                        """{"id":"a1","name":"An Author","libraryId":"lib","addedAt":0,"updatedAt":0,
                           "libraryItems":[${book("b1", "An Invented Book")}],"series":[]}"""
                    )
                    path.startsWith("/api/libraries/lib/items") -> MockResponse().setBody(
                        """{"results":[${book("b2", "A Read Book")}],"total":1}"""
                    )
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }
        server.start()
        SharedPreferencesHandler(context).saveUserPreferences(
            UserPreferences(server.url("/").toString().trimEnd('/'), "user", "token", false)
        )
        ApiClient.initialize(context)
        val bind = Intent(MediaSessionService.SERVICE_INTERFACE).setClass(context, PlaybackService::class.java)
        shadowOf(context as Application).setComponentNameAndServiceForBindService(
            ComponentName(context, PlaybackService::class.java),
            service.get().onBind(bind)
        )
    }

    @After
    fun tearDown() {
        scenario?.close()
        service.destroy()
        ApiClient.clearSession()
        server.shutdown()
    }

    private fun open(intent: Intent, shows: String) {
        scenario = ActivityScenario.launch(intent)
        compose.waitUntil(30_000) { compose.onAllNodesWithText(shows, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun activity(): Activity {
        var found: Activity? = null
        scenario!!.onActivity { found = it }
        return found!!
    }

    @Test
    fun `an author's page has the side menu, on Authors`() {
        open(AuthorDetailActivity.createIntent(context, "a1", PlayerPreview("An Author")), shows = "An Invented Book")

        compose.onNodeWithTag(menuItemTestTag("authors")).assertIsSelected()
    }

    @Test
    fun `a section chosen on the page opens the main screen on it`() {
        open(AuthorDetailActivity.createIntent(context, "a1"), shows = "An Invented Book")

        compose.onNodeWithTag(menuItemTestTag("series")).performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()

        val started = shadowOf(activity()).nextStartedActivity
        assertNotNull("nothing was started", started)
        assertEquals(MainActivity::class.java.name, started.component?.className)
        assertEquals("series", MainActivity.sectionOf(started))
        assertTrue(activity().isFinishing)
    }

    @Test
    fun `Back leaves the page, as on the book page`() {
        open(AuthorDetailActivity.createIntent(context, "a1"), shows = "An Invented Book")

        scenario!!.onActivity { it.onBackPressedDispatcher.onBackPressed() }

        assertTrue(activity().isFinishing)
    }

    @Test
    fun `a narrator's page lists the books they read, on Narrators`() {
        open(AuthorDetailActivity.narratorIntent(context, "lib", "A Reader"), shows = "A Read Book")

        compose.onNodeWithTag(menuItemTestTag("narrators")).assertIsSelected()
        compose.onAllNodesWithText("Reads 1 book").fetchSemanticsNodes().let { assertEquals(1, it.size) }
        assertTrue(asked.toString(), synchronized(asked) { asked.any { it.contains("filter=narrators.QSBSZWFkZXI%3D") } })
    }
}
