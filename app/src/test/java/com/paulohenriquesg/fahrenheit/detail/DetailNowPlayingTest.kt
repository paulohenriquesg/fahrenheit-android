package com.paulohenriquesg.fahrenheit.detail

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The details screen carries Now playing above itself, for a book and a podcast (#159). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w960dp-h540dp")
class DetailNowPlayingTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun showAndCheck(isBook: Boolean) {
        compose.setContent {
            FahrenheitTheme {
                // The content keeps the margin it is handed, as a book's does.
                DetailBody(isBook, nowPlaying = { BasicText("bar", Modifier.testTag("bar").width(200.dp).height(40.dp)) }) { margin ->
                    Box(Modifier.padding(margin).testTag("content")) { BasicText("the screen") }
                }
            }
        }
        compose.waitForIdle()
        val bar = compose.onNodeWithTag("bar").getUnclippedBoundsInRoot()
        val content = compose.onNodeWithTag("content").getUnclippedBoundsInRoot()
        assertTrue("bar $bar, content $content", bar.bottom <= content.top)
    }

    @Test fun `a book's screen has the bar above it`() = showAndCheck(isBook = true)

    @Test fun `a podcast's screen has the bar above it`() = showAndCheck(isBook = false)
}
