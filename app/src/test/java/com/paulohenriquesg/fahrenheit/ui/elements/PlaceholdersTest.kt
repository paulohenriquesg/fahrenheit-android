package com.paulohenriquesg.fahrenheit.ui.elements

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.isFocusable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.tv.material3.MaterialTheme
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The one shape every list draws while it loads (#224): still, grey in the
 * theme's own placeholder tone, and never a focus target.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w960dp-h540dp")
class PlaceholdersTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `a shape is drawn in the theme's placeholder tone`() {
        var tone = Color.Unspecified
        compose.setContent {
            FahrenheitTheme {
                tone = CoverPlaceholderTone.of(MaterialTheme.colorScheme)
                PlaceholderShape(Modifier.fillMaxSize())
            }
        }
        compose.waitForIdle()

        val map = window()
        val drawn = map[map.width / 2, map.height / 2]
        assertEquals("red", tone.red, drawn.red, 0.02f)
        assertEquals("green", tone.green, drawn.green, 0.02f)
        assertEquals("blue", tone.blue, drawn.blue, 0.02f)
    }

    @Test
    fun `none of the loading layouts can take focus`() {
        compose.setContent {
            FahrenheitTheme {
                androidx.compose.foundation.layout.Column {
                    ShelvesPlaceholder()
                    GridPlaceholder(GridPlaceholderLook.People)
                    RowsPlaceholder()
                }
            }
        }
        compose.waitForIdle()

        assertTrue(compose.onAllNodesWithTag(PlaceholderTags.SHAPE).fetchSemanticsNodes().isNotEmpty())
        compose.onAllNodes(isFocusable()).assertCountEquals(0)
    }

    @Test
    fun `people are drawn with round photos`() {
        compose.setContent { FahrenheitTheme { GridPlaceholder(GridPlaceholderLook.People) } }
        compose.waitForIdle()

        assertTrue(compose.onAllNodesWithTag(PlaceholderTags.ROUND).fetchSemanticsNodes().isNotEmpty())
    }

    @Test
    fun `covers are drawn square, not round`() {
        compose.setContent { FahrenheitTheme { GridPlaceholder(GridPlaceholderLook.Covers) } }
        compose.waitForIdle()

        compose.onAllNodesWithTag(PlaceholderTags.ROUND).assertCountEquals(0)
    }

    @Test
    fun `a shelf's heading takes the height of the real one, so the covers do not move`() {
        compose.setContent {
            FahrenheitTheme {
                androidx.compose.foundation.layout.Row {
                    ShelfHeading("An Invented Shelf")
                    ShelvesPlaceholder()
                }
            }
        }
        compose.waitForIdle()

        val real = compose.onNodeWithText("An Invented Shelf").getUnclippedBoundsInRoot().let { it.bottom - it.top }
        val placeholder = compose.onAllNodesWithTag(PlaceholderTags.HEADING)[0].getUnclippedBoundsInRoot().let { it.bottom - it.top }
        assertEquals(real.value, placeholder.value, 0.5f)
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
