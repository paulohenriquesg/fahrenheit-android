package com.paulohenriquesg.fahrenheit.ui.elements

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * What the wall draws, read off rendered pixels (#176): whole tilted covers to
 * every edge at both ends of the drift. Robolectric draws in software, so this
 * pins the drawing's geometry, not the stick's renderer.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w960dp-h540dp")
class CoverWallTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun white(): ImageBitmap =
        Bitmap.createBitmap(40, 40, Bitmap.Config.ARGB_8888).apply { eraseColor(android.graphics.Color.WHITE) }.asImageBitmap()

    private fun render() {
        // The wall drifts for as long as it is drawn; time here is moved by hand.
        compose.mainClock.autoAdvance = false
        compose.setContent {
            Box(Modifier.fillMaxSize().background(Color.Black)) {
                CoverWall(List(3) { white() }, Modifier.fillMaxSize(), alpha = 1f)
            }
        }
        compose.mainClock.advanceTimeByFrame()
    }

    // captureToImage waits for a frame callback Robolectric never sends, so
    // the screen is drawn into a bitmap directly.
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

    private fun lit(map: PixelMap, x: Int, y: Int) = map[x, y].red > 0.5f

    /** Cover in every corner, and no row or column of the screen bare from edge to edge. */
    private fun assertCovered(map: PixelMap, at: String) {
        val corner = 16
        val corners = listOf(0 to 0, map.width - corner to 0, 0 to map.height - corner, map.width - corner to map.height - corner)
        corners.forEach { (cx, cy) ->
            val any = (cx until cx + corner).any { x -> (cy until cy + corner).any { y -> lit(map, x, y) } }
            assertTrue("$at: no cover in the corner at $cx,$cy", any)
        }
        // Untilted, the gaps between covers would run straight across the screen.
        (0 until map.width).forEach { x ->
            assertTrue("$at: column $x is bare", (0 until map.height).any { y -> lit(map, x, y) })
        }
        (0 until map.height).forEach { y ->
            assertTrue("$at: row $y is bare", (0 until map.width).any { x -> lit(map, x, y) })
        }
    }

    @Test
    fun `whole tilted covers reach every edge at both ends of the drift`() {
        render()
        assertCovered(snapshot(), "start of the drift")

        compose.mainClock.advanceTimeBy(70_000)
        assertCovered(snapshot(), "end of the drift")
    }
}
