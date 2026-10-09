package com.paulohenriquesg.fahrenheit.favourites

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The heart in the player's actions (#180): a round icon button the size of
 * the skip buttons, so the actions beside it keep their width.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w960dp-h540dp")
class FavouriteButtonTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `not in, it offers to favourite`() {
        compose.setContent { FahrenheitTheme { FavouriteButton(filled = false, onClick = {}) } }

        compose.onNodeWithTag(FAVOURITE_BUTTON_TAG).assertContentDescriptionEquals("Favourite")
    }

    @Test
    fun `in, it offers to take it out of the playlist`() {
        compose.setContent { FahrenheitTheme { FavouriteButton(filled = true, onClick = {}, playlist = "Keepers") } }

        compose.onNodeWithTag(FAVOURITE_BUTTON_TAG).assertContentDescriptionEquals("Remove from Keepers")
    }

    @Test
    fun `it is a round icon the size of the skip buttons, with no label`() {
        compose.setContent { FahrenheitTheme { FavouriteButton(filled = false, onClick = {}) } }

        compose.onNodeWithTag(FAVOURITE_BUTTON_TAG).assertWidthIsEqualTo(48.dp).assertHeightIsEqualTo(48.dp)
        compose.onNodeWithText("Favourite").assertDoesNotExist()
    }

    @Test
    fun `Center presses it`() {
        var pressed = 0
        compose.setContent { FahrenheitTheme { FavouriteButton(filled = false, onClick = { pressed++ }) } }

        compose.onNodeWithTag(FAVOURITE_BUTTON_TAG).performSemanticsAction(SemanticsActions.OnClick)

        assertEquals(1, pressed)
    }
}
