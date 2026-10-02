package com.paulohenriquesg.fahrenheit.ui.theme

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.paulohenriquesg.fahrenheit.ui.Contrast
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The app theme configures the TV MaterialTheme, but text fields, progress
 * indicators and icons come from the phone one, which has its own
 * MaterialTheme. Left unset it falls back to the default light scheme, which is
 * how the dark login screen ended up with near-white slabs for fields and
 * white text on top of them.
 */
@RunWith(RobolectricTestRunner::class)
class PhoneComponentColoursTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun phoneScheme(dark: Boolean): Pair<Color, Color> {
        ThemeManager.setTheme(context, isDark = dark)
        var surface = Color.Unspecified
        var onSurface = Color.Unspecified
        compose.setContent {
            FahrenheitTheme {
                surface = androidx.compose.material3.MaterialTheme.colorScheme.surface
                onSurface = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
            }
        }
        compose.waitForIdle()
        return surface to onSurface
    }

    private fun contentOnBackground(dark: Boolean): Pair<Color, Color> {
        ThemeManager.setTheme(context, isDark = dark)
        var content = Color.Unspecified
        var background = Color.Unspecified
        compose.setContent {
            FahrenheitTheme {
                content = androidx.tv.material3.LocalContentColor.current
                background = androidx.tv.material3.MaterialTheme.colorScheme.background
            }
        }
        compose.waitForIdle()
        return content to background
    }

    @Test
    fun `text that names no colour is still legible in the dark theme`() {
        // The greeting passed no colour, so it took LocalContentColor, which
        // defaults to near-black: dark grey on a dark background.
        val (content, background) = contentOnBackground(dark = true)

        val ratio = Contrast.ratio(content, background)
        assertTrue("contrast ratio of $ratio", ratio >= 4.5f)
    }

    @Test
    fun `text that names no colour is still legible in the light theme`() {
        val (content, background) = contentOnBackground(dark = false)

        val ratio = Contrast.ratio(content, background)
        assertTrue("contrast ratio of $ratio", ratio >= 4.5f)
    }

    @Test
    fun `a phone surface is dark in the dark theme`() {
        val (surface, _) = phoneScheme(dark = true)

        assertTrue("surface luminance ${surface.luminance()}", surface.luminance() < 0.2f)
    }

    @Test
    fun `a phone surface is light in the light theme`() {
        val (surface, _) = phoneScheme(dark = false)

        assertTrue("surface luminance ${surface.luminance()}", surface.luminance() > 0.6f)
    }

    @Test
    fun `text on a phone surface is readable in the dark theme`() {
        val (surface, onSurface) = phoneScheme(dark = true)

        val ratio = Contrast.ratio(onSurface, surface)
        assertTrue("contrast ratio of $ratio", ratio >= 4.5f)
    }

    @Test
    fun `text on a phone surface is readable in the light theme`() {
        val (surface, onSurface) = phoneScheme(dark = false)

        val ratio = Contrast.ratio(onSurface, surface)
        assertTrue("contrast ratio of $ratio", ratio >= 4.5f)
    }
}
