package com.paulohenriquesg.fahrenheit.player

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs

/**
 * Ready (#223, frame 2 of docs/mocks/player-loading.html): the loading frame
 * has the plain background, and the cover's wash fades in over about 300 ms
 * once the player shows, rather than appearing at once.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w960dp-h540dp")
class PlayerWashFadeTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val background = Color(0xFF121212)
    private val wash = Color(0xFF7A3418)
    private val book = NowPlaying("b1", "An Invented Book", null, null, null, null, false, null)

    // Drawn by hand: captureToImage waits for a frame, which a paused clock never gives.
    private fun corner(): Color {
        val view = compose.activity.window.decorView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        compose.runOnUiThread { view.draw(Canvas(bitmap)) }
        return Color(bitmap.getPixel(view.width / 2, view.height / 4))
    }

    private fun near(a: Color, b: Color) =
        abs(a.red - b.red) < 0.02f && abs(a.green - b.green) < 0.02f && abs(a.blue - b.blue) < 0.02f

    @Test
    fun `the wash fades in from the background once the player shows`() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MaterialTheme(colorScheme = darkColorScheme(background = background)) {
                Box(Modifier.fillMaxSize().background(background)) {
                    PlayerScreen(nowPlaying = book, currentTime = 0.0, wash = wash, transport = {})
                }
            }
        }
        compose.mainClock.advanceTimeByFrame()
        val first = corner()

        compose.mainClock.advanceTimeBy(400)
        val at400 = corner()
        compose.mainClock.advanceTimeBy(1000)
        val settled = corner()

        assertTrue("opens on the plain background, not $first", near(first, background))
        assertTrue("the wash has come in", !near(settled, background))
        assertTrue("in about 300 ms: settled by 400 ms, not still at $at400", near(at400, settled))
    }
}
