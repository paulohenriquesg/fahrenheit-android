package com.paulohenriquesg.fahrenheit.ui.elements

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.tv.material3.ColorScheme
import androidx.tv.material3.MaterialTheme
import com.paulohenriquesg.fahrenheit.ui.Contrast
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import com.paulohenriquesg.fahrenheit.ui.theme.ThemeManager
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The no-cover placeholder has a tone of its own (#170). It was the focused
 * card's fill, so on a focused card the cover's edge disappeared.
 */
@RunWith(RobolectricTestRunner::class)
class CoverPlaceholderToneTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun scheme(dark: Boolean): ColorScheme {
        ThemeManager.setTheme(context, isDark = dark)
        var scheme: ColorScheme? = null
        compose.setContent { FahrenheitTheme { scheme = MaterialTheme.colorScheme } }
        compose.waitForIdle()
        return scheme!!
    }

    private fun check(dark: Boolean) {
        val c = scheme(dark)
        val tone = CoverPlaceholderTone.of(c)
        fun apart(what: String, other: Color, at: Float) =
            Contrast.ratio(tone, other).let { assertTrue("$what: ${"%.2f".format(it)}:1", it >= at) }
        // A focused card is filled with surfaceVariant, an unfocused one with
        // surface. In the dark theme those two are only 1.68:1 apart, so a tone
        // between them can be about 1.3:1 from each and no more.
        apart("against a focused card", c.surfaceVariant, 1.25f)
        apart("against a card at rest", c.surface, 1.25f)
        // And the placeholder's own words still read on it.
        apart("its title", c.onSurface, 4.5f)
        apart("its author", c.onSurfaceVariant, 4.5f)
    }

    @Test fun `the dark theme's placeholder stands apart from the cards, and reads`() = check(dark = true)

    @Test fun `the light theme's placeholder stands apart from the cards, and reads`() = check(dark = false)
}
