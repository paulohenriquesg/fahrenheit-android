package com.paulohenriquesg.fahrenheit.ui.elements

import android.content.Context
import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

/**
 * The covers this device has drawn, kept by address so the login screen can
 * show them before anyone is signed in (#161) - from what is already on the
 * device, without asking the server for anything.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w960dp-h540dp")
class RecentCoversTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val server = MockWebServer().apply { start() }
    private val host = server.url("/").toString().trimEnd('/')

    @After
    fun tearDown() = server.shutdown()

    private fun png(): Buffer {
        val bitmap = Bitmap.createBitmap(40, 40, Bitmap.Config.ARGB_8888).apply { eraseColor(android.graphics.Color.RED) }
        val bytes = ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
        return Buffer().write(bytes)
    }

    // Off the main thread, with the main looper turning meanwhile: Coil hands
    // its result back on the main thread, which runBlocking there would hold.
    private fun loadCached(): List<ImageBitmap> {
        val result = AtomicReference<List<ImageBitmap>>()
        thread { result.set(runBlocking { RecentCovers.loadCached(context, host) }) }
        val deadline = System.currentTimeMillis() + 10_000
        while (result.get() == null && System.currentTimeMillis() < deadline) {
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            Thread.sleep(10)
        }
        return result.get() ?: error("the covers did not load")
    }

    @Test
    fun `the covers of one server, newest first`() {
        RecentCovers.note(context, "http://one.local/api/items/a/cover")
        RecentCovers.note(context, "http://two.local/api/items/b/cover")
        RecentCovers.note(context, "http://one.local/api/items/c/cover")

        assertEquals(
            listOf("http://one.local/api/items/c/cover", "http://one.local/api/items/a/cover"),
            RecentCovers.forServer(context, "http://one.local")
        )
    }

    // A host that only starts like another is another server.
    @Test
    fun `a server whose address extends another's is not that server`() {
        RecentCovers.note(context, "http://one.local:8080/api/items/a/cover")

        assertEquals(emptyList<String>(), RecentCovers.forServer(context, "http://one.local"))
    }

    @Test
    fun `a cover drawn again moves to the front, once`() {
        RecentCovers.note(context, "http://one.local/api/items/a/cover")
        RecentCovers.note(context, "http://one.local/api/items/b/cover")
        RecentCovers.note(context, "http://one.local/api/items/a/cover")

        assertEquals(
            listOf("http://one.local/api/items/a/cover", "http://one.local/api/items/b/cover"),
            RecentCovers.forServer(context, "http://one.local")
        )
    }

    @Test
    fun `only the most recent are kept`() {
        repeat(RecentCovers.MAX + 10) { RecentCovers.note(context, "http://one.local/api/items/i$it/cover") }

        val kept = RecentCovers.forServer(context, "http://one.local")
        assertEquals(RecentCovers.MAX, kept.size)
        assertEquals("http://one.local/api/items/i${RecentCovers.MAX + 9}/cover", kept.first())
    }

    @Test
    fun `a host with a trailing slash finds the same covers`() {
        RecentCovers.note(context, "http://one.local/api/items/a/cover")

        assertEquals(1, RecentCovers.forServer(context, "http://one.local/").size)
    }

    // An address typed with a trailing slash is kept as typed, and the
    // cover's URL then has two: it is still that server's cover.
    @Test
    fun `a cover fetched through an address ending in a slash is found`() {
        RecentCovers.note(context, "http://one.local//api/items/a/cover")

        assertEquals(listOf("http://one.local//api/items/a/cover"), RecentCovers.forServer(context, "http://one.local/"))
        assertEquals(listOf("http://one.local//api/items/a/cover"), RecentCovers.forServer(context, "http://one.local"))
    }

    // The whole way: drawn once while signed in, then shown at sign-in from
    // the device alone.
    @Test
    fun `a cover drawn is shown again later without asking the server`() {
        server.enqueue(MockResponse().setBody(png()).setHeader("Content-Type", "image/png"))
        compose.setContent {
            FahrenheitTheme {
                CoverImage(itemId = "b1", contentDescription = "An Invented Book", size = 100.dp, url = "$host/api/items/b1/cover")
            }
        }
        server.takeRequest(5, TimeUnit.SECONDS)!!
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag(CoverTags.LOADED, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
        assertEquals(listOf("$host/api/items/b1/cover"), RecentCovers.forServer(context, host))

        val covers = loadCached()

        assertEquals(1, covers.size)
        assertEquals("no request before sign-in", 1, server.requestCount)
    }

    @Test
    fun `a cover never drawn here is not fetched`() {
        RecentCovers.note(context, "$host/api/items/never/cover")
        server.enqueue(MockResponse().setBody(png()).setHeader("Content-Type", "image/png"))

        val covers = loadCached()

        assertTrue(covers.isEmpty())
        assertEquals(0, server.requestCount)
    }
}
