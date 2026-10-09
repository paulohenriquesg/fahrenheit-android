package com.paulohenriquesg.fahrenheit.player

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * The Stay answer's outline follows its own 16 dp corners (#209), not the
 * library's pill: read off the drawn window.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w960dp-h540dp")
class ResumeChoiceLookTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val item = ResumeItem(itemId = "b1", title = "A Made-Up Book", length = 7200.0, chapters = emptyList(), episode = false)
    private val offer = ResumeOffer(here = 1800.0, there = 3600.0, listenedAt = 0L, device = "iPhone")

    private fun show() {
        compose.setContent {
            FahrenheitTheme { ResumeChoice(offer, item, now = 60_000L, onContinue = {}, onStay = {}) }
        }
        compose.waitForIdle()
    }

    /** Where a corner's outline runs, at 45°: [radius] in, less half the stroke. */
    private fun onArc(radiusDp: Float, strokeDp: Float): Float =
        radiusDp - (radiusDp - strokeDp / 2) / sqrt(2f)

    /**
     * The Stay answer's pixels around ([x], [y]) dp from its top-left, as
     * drawn: grown by [scale] about its centre, as a focused button is. Three
     * by three, as a stroke this thin may fall either side of one pixel.
     */
    private fun stayAround(map: PixelMap, x: Float, y: Float, scale: Float = 1f): List<Color> {
        val node = compose.onNodeWithTag("resume_stay").fetchSemanticsNode()
        val density = compose.density.density
        val at = node.positionInWindow
        val cx = node.size.width / 2f
        val cy = node.size.height / 2f
        val px = (at.x + cx + (x * density - cx) * scale).roundToInt()
        val py = (at.y + cy + (y * density - cy) * scale).roundToInt()
        return (-1..1).flatMap { dy -> (-1..1).map { dx -> map[px + dx, py + dy] } }
    }

    private fun heightDp() = compose.onNodeWithTag("resume_stay").fetchSemanticsNode().size.height / compose.density.density

    private fun differ(a: Color, b: Color) =
        maxOf(abs(a.red - b.red), abs(a.green - b.green), abs(a.blue - b.blue)) > 0.03f

    /** Whether an outline runs through [pixels], against a bare [ground]. */
    private fun outlined(pixels: List<Color>, ground: Color) = pixels.any { differ(it, ground) }

    @Test
    fun `unfocused, the outline follows the 16 dp corners`() {
        show()
        val map = window()

        val outside = stayAround(map, 0.5f, 0.5f)[4]
        val corner = onArc(16f, 1.5f)
        assertTrue("no outline on the 16 dp corner", outlined(stayAround(map, corner, corner), outside))
        // Inside the corner the container is clear: the card shows through.
        val pill = onArc(heightDp() / 2, 1.5f)
        assertFalse("an outline on the pill's arc", outlined(stayAround(map, pill, pill), outside))
    }

    @Test
    fun `focused, no pill-shaped outline crosses the corners`() {
        show()
        compose.onNodeWithTag("resume_stay").performSemanticsAction(SemanticsActions.RequestFocus)
        compose.waitForIdle()
        val map = window()
        val grown = 1.1f

        val filled = stayAround(map, heightDp() / 2, 5f, grown)[4]
        val pill = onArc(heightDp() / 2, 1.65f)
        assertFalse("an outline on the pill's arc", outlined(stayAround(map, pill, pill, grown), filled))
        val corner = onArc(16f, 1.65f)
        assertTrue("no outline on the 16 dp corner", outlined(stayAround(map, corner, corner, grown), filled))
    }

    // The window drawn into a bitmap (captureToImage waits for a frame callback
    // Robolectric never sends).
    private fun window(): PixelMap {
        lateinit var map: PixelMap
        compose.runOnUiThread {
            val view = compose.activity.window.decorView
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            map = bitmap.asImageBitmap().toPixelMap()
        }
        return map
    }
}
