package com.paulohenriquesg.fahrenheit.main

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import com.paulohenriquesg.fahrenheit.api.Shelf
import com.paulohenriquesg.fahrenheit.ui.elements.PlaceholderTags
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * Switching library showed the shelves of the library you had just left for a
 * second or two, until the new ones arrived. Home draws placeholder shelves
 * instead (#224), where it used to say "Loading…".
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HomeLoadingTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun shelf() = Shelf(
        id = "recent",
        label = "Recently Added",
        labelStringKey = "labels_recently_added",
        type = "book"
    )

    @Test
    fun `while a library is loading, Home draws placeholders rather than the last one`() {
        compose.setContent {
            FahrenheitTheme {
                PersonalizedHomeView(shelves = emptyList(), libraryId = "lib", isLoading = true)
            }
        }

        assertTrue(compose.onAllNodesWithTag(PlaceholderTags.SHAPE).fetchSemanticsNodes().isNotEmpty())
    }

    @Test
    fun `an empty library that has finished loading does not claim to be loading`() {
        compose.setContent {
            FahrenheitTheme {
                PersonalizedHomeView(shelves = emptyList(), libraryId = "lib", isLoading = false)
            }
        }

        compose.onAllNodes(hasText("Loading", substring = true)).assertCountEquals(0)
    }

    @Test
    fun `shelves that have arrived are shown, not a loading line`() {
        compose.setContent {
            FahrenheitTheme {
                PersonalizedHomeView(shelves = listOf(shelf()), libraryId = "lib", isLoading = false)
            }
        }

        compose.onAllNodes(hasText("Loading", substring = true)).assertCountEquals(0)
    }
}
