package com.paulohenriquesg.fahrenheit.ui.elements

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.activity.ComponentActivity
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Box
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

/**
 * A cover drawn the way the server's own web client draws it (#149): a
 * placeholder with the title and author when there is no cover or it fails to
 * load, and a cover of another shape fitted whole over a dimmed copy of
 * itself rather than cropped.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w960dp-h540dp")
class CoverImageTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val server = MockWebServer().apply { start() }

    @After
    fun tearDown() = server.shutdown()

    private fun render() {
        compose.setContent {
            FahrenheitTheme {
                Box(Modifier.testTag("cover")) {
                    CoverImage(
                        itemId = "b1", contentDescription = "An Invented Book", size = 100.dp,
                        title = "An Invented Book", author = "An Invented Writer",
                        url = server.url("/api/items/b1/cover").toString()
                    )
                }
            }
        }
        // The cover is asked for when first drawn; wait for that request, and
        // for the answer, before anything is asserted.
        compose.waitForIdle()
        server.takeRequest(5, TimeUnit.SECONDS)!!
    }

    private fun waitForPlaceholder(shown: Boolean) =
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag(CoverTags.PLACEHOLDER, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() == shown
        }

    private fun png(width: Int, height: Int, draw: (Canvas) -> Unit): Buffer {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        draw(Canvas(bitmap))
        val bytes = ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
        return Buffer().write(bytes)
    }

    @Test
    fun `an item without a cover shows its title and author`() {
        server.enqueue(MockResponse().setResponseCode(404))

        render()
        compose.waitForIdle()

        waitForPlaceholder(shown = true)
        compose.onNodeWithTag(CoverTags.PLACEHOLDER, useUnmergedTree = true)
            .assert(SemanticsMatcher.expectValue(CoverPlaceholderLines, listOf("An Invented Book", "An Invented Writer")))
    }

    @Test
    fun `a cover that fails to load shows the same placeholder`() {
        server.enqueue(MockResponse().setBody("not an image").setHeader("Content-Type", "image/png"))

        render()
        compose.waitForIdle()

        waitForPlaceholder(shown = true)
        compose.onNodeWithTag(CoverTags.PLACEHOLDER, useUnmergedTree = true)
            .assert(SemanticsMatcher.expectValue(CoverPlaceholderLines, listOf("An Invented Book", "An Invented Writer")))
    }

    @Test
    fun `a cover that loads replaces the placeholder`() {
        server.enqueue(MockResponse().setBody(png(40, 40) { it.drawColor(android.graphics.Color.RED) }).setHeader("Content-Type", "image/png"))

        render()

        waitForPlaceholder(shown = false)
    }

    // A podcast's cover is often not square. Cropped, its left edge was cut
    // off and the square filled; fitted, the whole image shows in a band.
    @Test
    fun `a wide cover is fitted whole, not cropped to the square`() {
        val wide = png(400, 100) { canvas ->
            canvas.drawColor(android.graphics.Color.BLUE)
            canvas.drawRect(0f, 0f, 100f, 100f, Paint().apply { color = android.graphics.Color.RED })
        }
        server.enqueue(MockResponse().setBody(wide).setHeader("Content-Type", "image/png"))

        render()
        waitForPlaceholder(shown = false)
        // Past the cover's crossfade, on both the compose and system clocks.
        compose.mainClock.advanceTimeBy(1_000)
        org.robolectric.shadows.ShadowLooper.idleMainLooper(1, TimeUnit.SECONDS)
        compose.waitForIdle()

        val node = compose.onNodeWithTag("cover").fetchSemanticsNode()
        val at = node.positionInWindow
        // Robolectric draws only when asked, and the crossfade starts at the
        // first draw: draw once, let it run, then read the second drawing.
        snapshot()
        org.robolectric.shadows.ShadowLooper.idleMainLooper(1, TimeUnit.SECONDS)
        val window = snapshot()
        val middleLeft = window[at.x.toInt() + 2, at.y.toInt() + node.size.height / 2]
        val topLeft = window[at.x.toInt() + 2, at.y.toInt() + 2]
        // The image's own left quarter, red, is on screen in full...
        assertEquals("middle left $middleLeft", Color.Red.toArgbHex(), middleLeft.toArgbHex())
        // ...and the top-left corner is not the image itself but what lies
        // behind the fitted band. Cropped, this pixel would be the same red.
        assertNotEquals("top left $topLeft", Color.Red.toArgbHex(), topLeft.toArgbHex())
    }

    // captureToImage waits for a frame callback Robolectric never sends (as
    // LoginLookTest found), so the window is drawn into a bitmap directly.
    private fun snapshot(): PixelMap {
        var map: PixelMap? = null
        compose.runOnUiThread {
            val view = compose.activity.window.decorView
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            map = bitmap.asImageBitmap().toPixelMap()
        }
        return map!!
    }

    private fun Color.toArgbHex(): String =
        "%08X".format(
            ((alpha * 255).toInt() shl 24) or ((red * 255).toInt() shl 16) or ((green * 255).toInt() shl 8) or (blue * 255).toInt()
        )

    // A screen reader hears the cover's description once, not the
    // placeholder's lines again on top of it.
    @Test
    fun `the placeholder's lines are not read out`() {
        server.enqueue(MockResponse().setResponseCode(404))

        render()
        waitForPlaceholder(shown = true)

        compose.onNodeWithText("An Invented Writer", useUnmergedTree = true).assertDoesNotExist()
    }
}
