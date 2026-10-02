package com.paulohenriquesg.fahrenheit.ui.theme

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.tv.material3.MaterialTheme
import com.paulohenriquesg.fahrenheit.ui.Contrast
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Every pair the app paints text with, checked rather than eyeballed.
 *
 * "Play Book" was white on light purple: in a dark scheme `primary` is a light
 * tone, so its content colour has to be dark, and the theme had white for
 * every "on" role regardless.
 */
@RunWith(RobolectricTestRunner::class)
class PaletteContrastTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun failingPairs(dark: Boolean): List<String> {
        ThemeManager.setTheme(context, isDark = dark)
        val pairs = mutableListOf<Triple<String, Color, Color>>()
        compose.setContent {
            FahrenheitTheme {
                val c = MaterialTheme.colorScheme
                pairs += Triple("background", c.onBackground, c.background)
                pairs += Triple("surface", c.onSurface, c.surface)
                pairs += Triple("surfaceVariant", c.onSurfaceVariant, c.surfaceVariant)
                pairs += Triple("primary", c.onPrimary, c.primary)
                pairs += Triple("secondary", c.onSecondary, c.secondary)
                pairs += Triple("tertiary", c.onTertiary, c.tertiary)
                pairs += Triple("secondaryContainer", c.onSecondaryContainer, c.secondaryContainer)
                pairs += Triple("tertiaryContainer", c.onTertiaryContainer, c.tertiaryContainer)
            }
        }
        compose.waitForIdle()
        return pairs.mapNotNull { (name, content, container) ->
            val ratio = Contrast.ratio(content, container)
            // 4.5:1 is the readable threshold; these are all text pairs.
            if (ratio < 4.5f) "$name (${"%.2f".format(ratio)}:1)" else null
        }
    }

    @Test
    fun `every role pair is readable in the dark theme`() =
        assertEquals(emptyList<String>(), failingPairs(dark = true))

    @Test
    fun `every role pair is readable in the light theme`() =
        assertEquals(emptyList<String>(), failingPairs(dark = false))
}
