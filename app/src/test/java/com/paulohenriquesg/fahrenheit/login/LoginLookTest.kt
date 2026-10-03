package com.paulohenriquesg.fahrenheit.login

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.graphics.asImageBitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.core.app.ApplicationProvider
import androidx.tv.material3.MaterialTheme
import com.paulohenriquesg.fahrenheit.storage.SharedPreferencesHandler
import com.paulohenriquesg.fahrenheit.ui.Contrast
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import com.paulohenriquesg.fahrenheit.ui.theme.ThemeManager
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * What the welcome-back buttons look like, read off rendered pixels.
 *
 * On the stick, moving down from the password field left no button looking
 * focused: the TV button's resting fill is 80% white and its focused fill is
 * 100% white. Frame 3 of the login mock has Sign in filled with the primary
 * colour, the others secondary, and a ring in the primary colour on focus.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w960dp-h540dp")
class LoginLookTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val buttons = listOf("login_submit_button", "login_mode_toggle", "login_different_server")
    private var primary = Color.Unspecified

    private fun show(dark: Boolean) {
        ThemeManager.setTheme(context, isDark = dark)
        val prefs = SharedPreferencesHandler(context)
        prefs.saveUserPreferences(
            prefs.getUserPreferences().copy(host = "http://abs.local:13378", username = "someone")
        )
        compose.setContent {
            FahrenheitTheme {
                primary = MaterialTheme.colorScheme.primary
                // LoginActivity draws the screen on the theme's background.
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    LoginScreen({ _, _, _, _ -> }, { _, _, _ -> })
                }
            }
        }
        compose.waitForIdle()
    }

    // The focused field's cursor blinks forever, so the clock is driven by
    // hand: on its own a capture would wait for an idle that never comes.
    private fun focus(tag: String) {
        compose.mainClock.autoAdvance = false
        compose.onNodeWithTag(tag).performSemanticsAction(SemanticsActions.RequestFocus)
        compose.mainClock.advanceTimeBy(1_000)
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

    /**
     * The middle of the button, clear of its edge. A focused TV button also
     * grows a little; counting the pixels it grows into let the old 80% to
     * 100% white change pass as visible, which on the stick it was not.
     */
    private fun inside(tag: String): Rect {
        val bounds = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
        return bounds.deflate(minOf(bounds.width, bounds.height) * 0.2f)
    }

    private fun PixelMap.colours(area: Rect): List<Color> {
        val out = ArrayList<Color>()
        for (y in area.top.roundToInt().coerceAtLeast(0) until area.bottom.roundToInt().coerceAtMost(height)) {
            for (x in area.left.roundToInt().coerceAtLeast(0) until area.right.roundToInt().coerceAtMost(width)) {
                out += this[x, y]
            }
        }
        return out
    }

    private fun dominant(tag: String): Color {
        val inside = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
        return snapshot().colours(inside)
            .groupingBy { it }.eachCount().maxBy { it.value }.key
    }

    private fun distance(a: Color, b: Color) =
        (abs(a.red - b.red) + abs(a.green - b.green) + abs(a.blue - b.blue)) * 255

    private fun assertFocusShows(dark: Boolean) {
        show(dark)
        for (tag in buttons) {
            val area = inside(tag)
            focus("login_password_field")
            val before = snapshot().colours(area)
            focus(tag)
            val after = snapshot().colours(area)

            // WCAG 2.2 focus appearance asks for a 3:1 change between the
            // states. Most of the button's middle - its fill - has to make it.
            val changed = before.zip(after).count { (b, a) -> Contrast.ratio(b, a) >= 3f }
            assertTrue(
                "$tag (dark=$dark): $changed of ${before.size} pixels change by 3:1 on focus",
                changed >= before.size / 2
            )
        }
    }

    @Test
    fun `every button shows focus, in the dark theme`() = assertFocusShows(dark = true)

    @Test
    fun `every button shows focus, in the light theme`() = assertFocusShows(dark = false)

    @Test
    fun `Sign in is filled with the primary colour and the others are not`() {
        show(dark = true)
        focus("login_password_field")

        val signIn = dominant("login_submit_button")
        assertTrue("Sign in is $signIn, primary is $primary", distance(signIn, primary) < 12f)
        for (tag in buttons.drop(1)) {
            val fill = dominant(tag)
            assertTrue("$tag is $fill, too close to primary $primary", distance(fill, primary) > 60f)
        }
    }
}
