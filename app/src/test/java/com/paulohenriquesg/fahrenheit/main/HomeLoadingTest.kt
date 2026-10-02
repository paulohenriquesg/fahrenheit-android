package com.paulohenriquesg.fahrenheit.main

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.paulohenriquesg.fahrenheit.api.Shelf
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * Switching library showed the shelves of the library you had just left for a
 * second or two, until the new ones arrived. Home says it is loading instead.
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
    fun `while a library is loading, Home says so rather than showing the last one`() {
        compose.setContent {
            FahrenheitTheme {
                PersonalizedHomeView(shelves = emptyList(), libraryId = "lib", isLoading = true)
            }
        }

        compose.onNodeWithText("Loading", substring = true).assertIsDisplayed()
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
