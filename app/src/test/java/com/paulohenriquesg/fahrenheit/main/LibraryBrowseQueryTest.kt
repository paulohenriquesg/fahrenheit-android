package com.paulohenriquesg.fahrenheit.main

import androidx.activity.ComponentActivity
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.paulohenriquesg.fahrenheit.api.LibraryQuery
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Opened from a Home shelf's "See all", the library is a narrower list than
 * the whole library, and says which (#146); opened from the rail it is the
 * library as before.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w960dp-h540dp")
class LibraryBrowseQueryTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun render(query: LibraryQuery, loading: Boolean = false) {
        compose.setContent {
            FahrenheitTheme {
                LibraryBrowseView(
                    name = "An Invented Library", itemLabel = "books", items = emptyList(),
                    rowLayout = true, listState = rememberLazyListState(), query = query, loading = loading
                )
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun `recently added says so`() {
        render(LibraryQuery.RecentlyAdded)

        compose.onNodeWithText("An Invented Library").assertIsDisplayed()
        compose.onNodeWithText("Recently added", substring = true).assertIsDisplayed()
    }

    @Test
    fun `in progress says so`() {
        render(LibraryQuery.InProgress)

        compose.onNodeWithText("In progress", substring = true).assertIsDisplayed()
    }

    @Test
    fun `finished says so`() {
        render(LibraryQuery.Finished)

        compose.onNodeWithText("Finished", substring = true).assertIsDisplayed()
    }

    @Test
    fun `the whole library carries no label`() {
        render(LibraryQuery.Everything)

        compose.onNodeWithText("Recently added", substring = true).assertDoesNotExist()
        compose.onNodeWithText("In progress", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Finished", substring = true).assertDoesNotExist()
    }

    // An empty list while the fetch runs read as "nothing in progress".
    @Test
    fun `a view still loading says so rather than counting nothing`() {
        render(LibraryQuery.InProgress, loading = true)

        compose.onNodeWithText("Loading…", substring = true).assertIsDisplayed()
        compose.onNodeWithText("0 books", substring = true).assertDoesNotExist()
    }
}
