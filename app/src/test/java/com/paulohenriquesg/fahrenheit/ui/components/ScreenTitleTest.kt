package com.paulohenriquesg.fahrenheit.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w960dp-h540dp")
class ScreenTitleTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `shows its text`() {
        compose.setContent { FahrenheitTheme { ScreenTitle("Series") } }

        compose.onNodeWithText("Series").assertIsDisplayed()
    }

    @Test
    fun `shows what is put after it, beside it`() {
        compose.setContent {
            FahrenheitTheme {
                ScreenTitle("Audiobooks") { Text("(12 books)") }
            }
        }

        val title = compose.onNodeWithText("Audiobooks").assertIsDisplayed().getUnclippedBoundsInRoot()
        val count = compose.onNodeWithText("(12 books)").assertIsDisplayed().getUnclippedBoundsInRoot()
        assert(count.left >= title.right) { "the count starts at ${count.left}, inside the title ending at ${title.right}" }
    }

    @Test
    fun `a long title stays on one line`() {
        compose.setContent {
            FahrenheitTheme {
                androidx.compose.foundation.layout.Column {
                    ScreenTitle("Short", modifier = Modifier.width(200.dp).testTag("short"))
                    ScreenTitle(
                        "A library with a name far too long to fit on one line of a narrow box",
                        modifier = Modifier.width(200.dp).testTag("long")
                    )
                }
            }
        }

        val short = compose.onNodeWithTag("short").getUnclippedBoundsInRoot()
        val long = compose.onNodeWithTag("long").getUnclippedBoundsInRoot()
        assertEquals((short.bottom - short.top).value, (long.bottom - long.top).value, 0.5f)
    }
}
