package com.paulohenriquesg.fahrenheit.detail

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.media3.session.MediaSessionService
import com.paulohenriquesg.fahrenheit.player.PlaybackService
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.paulohenriquesg.fahrenheit.api.ApiClient
import com.paulohenriquesg.fahrenheit.api.ProgressMark
import com.paulohenriquesg.fahrenheit.progress.ProgressStore
import com.paulohenriquesg.fahrenheit.storage.SharedPreferencesHandler
import com.paulohenriquesg.fahrenheit.storage.UserPreferences
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The podcast page's rows read the shared store (#207): an episode the player
 * finished shows finished on Back, with no read of the page's own.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w960dp-h540dp")
class PodcastPageProgressTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val server = MockWebServer()
    private var scenario: ActivityScenario<DetailActivity>? = null
    // The page's Now playing bar connects to it, as on a device.
    private val service = Robolectric.buildService(PlaybackService::class.java).create()

    private val podcast = """{"id":"pod","ino":"1","libraryId":"lib","folderId":"f","path":"/p","relPath":"p",
        "isFile":false,"mtimeMs":0,"ctimeMs":0,"birthtimeMs":0,"addedAt":0,"updatedAt":0,"lastScan":0,
        "isMissing":false,"isInvalid":false,"mediaType":"podcast","libraryFiles":[],"size":0,
        "media":{"id":"m","libraryItemId":"pod","tags":[],"size":0,
          "metadata":{"title":"An Invented Show","explicit":false},
          "episodes":[{"libraryItemId":"pod","id":"e1","index":1,"title":"Episode One","publishedAt":1,"addedAt":1,"updatedAt":1}]}}"""

    @Before
    fun setUp() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when {
                request.path.orEmpty().startsWith("/api/items/pod") -> MockResponse().setBody(podcast)
                request.path.orEmpty().startsWith("/api/me") -> MockResponse().setBody("""{"type":"user","mediaProgress":[]}""")
                else -> MockResponse().setResponseCode(404)
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

    @Test
    fun `an episode finished in the player shows finished on the page`() {
        scenario = ActivityScenario.launch(DetailActivity.createIntent(context, "pod"))
        compose.waitUntil(30_000) { compose.onAllNodesWithContentDescription("Episode One", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodesWithContentDescription("Finished", useUnmergedTree = true).fetchSemanticsNodes().let {
            check(it.isEmpty()) { "finished before anything was played" }
        }

        compose.runOnIdle { ProgressStore.process.marked("pod", "e1", ProgressMark(isFinished = true)) }

        compose.waitUntil(5_000) { compose.onAllNodesWithContentDescription("Finished", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
    }
}
