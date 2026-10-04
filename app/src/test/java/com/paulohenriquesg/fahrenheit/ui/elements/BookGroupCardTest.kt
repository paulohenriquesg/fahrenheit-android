package com.paulohenriquesg.fahrenheit.ui.elements

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * A series or a collection shows its books' covers (#149), as the server's web
 * client does: a collection its first two side by side, a series up to three
 * fanned. No new requests: the server sends the books with each group.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w960dp-h540dp")
class BookGroupCardTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun render(look: BookGroupLook, books: Int) {
        compose.setContent {
            FahrenheitTheme {
                BookGroupCard(
                    name = "An Invented Group", bookCount = books,
                    coverIds = List(books) { "b$it" }, look = look, onClick = {}
                )
            }
        }
        compose.waitForIdle()
    }

    private fun assertCovers(count: Int) =
        compose.onAllNodesWithTag(CoverTags.GROUP_COVER, useUnmergedTree = true).assertCountEquals(count)

    @Test
    fun `a collection shows its first two covers`() {
        render(BookGroupLook.Collection, books = 5)

        assertCovers(2)
        compose.onNodeWithText("An Invented Group").assertIsDisplayed()
        compose.onNodeWithText("5 books", substring = true).assertIsDisplayed()
    }

    @Test
    fun `a collection of one book shows that one`() {
        render(BookGroupLook.Collection, books = 1)

        assertCovers(1)
    }

    @Test
    fun `an empty collection says so instead of covers`() {
        render(BookGroupLook.Collection, books = 0)

        assertCovers(0)
        compose.onNodeWithText("Empty collection", substring = true).assertExists()
    }

    @Test
    fun `a series fans up to three covers`() {
        render(BookGroupLook.Series, books = 7)

        assertCovers(3)
    }

    @Test
    fun `a series of two fans two`() {
        render(BookGroupLook.Series, books = 2)

        assertCovers(2)
    }

    @Test
    fun `a series with no covers is just its name`() {
        render(BookGroupLook.Series, books = 0)

        assertCovers(0)
        compose.onNodeWithText("An Invented Group").assertIsDisplayed()
        compose.onNodeWithText("Empty collection", substring = true).assertDoesNotExist()
    }
}
