package com.paulohenriquesg.fahrenheit.favourites

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The heart chip in the player's actions (#180). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w960dp-h540dp")
class FavouriteChipTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun state(text: String) = SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, text)

    @Test
    fun `it reads Favourite, and says whether this is in`() {
        compose.setContent { FahrenheitTheme { FavouriteChip(filled = true, onClick = {}) } }

        compose.onNodeWithText("Favourite").assertExists()
        compose.onNodeWithTag(FAVOURITE_CHIP_TAG).assert(state("In Favourites"))
    }

    @Test
    fun `not in, it says so`() {
        compose.setContent { FahrenheitTheme { FavouriteChip(filled = false, onClick = {}) } }

        compose.onNodeWithTag(FAVOURITE_CHIP_TAG).assert(state("Not in Favourites"))
    }

    @Test
    fun `Center presses it`() {
        var pressed = 0
        compose.setContent { FahrenheitTheme { FavouriteChip(filled = false, onClick = { pressed++ }) } }

        compose.onNodeWithTag(FAVOURITE_CHIP_TAG).performSemanticsAction(SemanticsActions.OnClick)

        assertEquals(1, pressed)
    }
}
