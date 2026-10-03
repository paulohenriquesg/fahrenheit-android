package com.paulohenriquesg.fahrenheit.ui.elements

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.paulohenriquesg.fahrenheit.api.Author
import com.paulohenriquesg.fahrenheit.api.Series
import com.paulohenriquesg.fahrenheit.api.Shelf
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Shelf headings at the mock's size (docs/mocks/screens.html, `.shelf h4`:
 * 32px at density 2, weight 700): 16 sp bold, not the 24 sp headline they were.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w960dp-h540dp")
class ShelfHeadingTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun shelf(type: String) = Shelf(
        id = "s", label = "An Invented Shelf", labelStringKey = "Label", type = type
    )

    private fun assertHeadingAtMockSize(content: @Composable () -> Unit) {
        compose.setContent { FahrenheitTheme { content() } }

        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText("An Invented Shelf").fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult].action!!.invoke(layouts)
        val style = layouts.single().layoutInput.style

        assertEquals(16.sp, style.fontSize)
        assertEquals(FontWeight.Bold, style.fontWeight)
    }

    @Test
    fun `a shelf of books heads itself at the mock's size`() =
        assertHeadingAtMockSize { ShelfRow(shelf("book")) {} }

    @Test
    fun `a shelf of authors heads itself at the mock's size`() =
        assertHeadingAtMockSize { AuthorShelfRow(shelf("authors"), listOf(Author(id = "a1", name = "A Writer"))) {} }

    @Test
    fun `a shelf of series heads itself at the mock's size`() =
        assertHeadingAtMockSize { SeriesShelfRow(shelf("series"), listOf(Series(id = "s1", name = "A Series"))) {} }
}
