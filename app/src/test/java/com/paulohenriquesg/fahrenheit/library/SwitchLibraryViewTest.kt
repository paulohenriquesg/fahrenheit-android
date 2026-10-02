package com.paulohenriquesg.fahrenheit.library

import androidx.activity.ComponentActivity
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import com.paulohenriquesg.fahrenheit.api.Library
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
class SwitchLibraryViewTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun library(id: String, name: String, mediaType: String) = Library(
        id = id,
        name = name,
        folders = null,
        displayOrder = null,
        icon = null,
        mediaType = mediaType,
        provider = null,
        settings = null,
        createdAt = null,
        lastUpdate = null,
        lastScan = null,
        lastScanVersion = null
    )

    private val libraries = listOf(
        library("lib-1", "Audiobooks", "book"),
        library("lib-2", "Podcasts", "podcast")
    )

    private fun render(current: String? = "lib-1", onSelect: (Library) -> Unit = {}) {
        compose.setContent {
            FahrenheitTheme {
                SwitchLibraryView(libraries = libraries, currentId = current, onSelect = onSelect)
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun `every library is offered`() {
        render()

        compose.onNodeWithText("Audiobooks").assertIsDisplayed()
        compose.onNodeWithText("Podcasts").assertIsDisplayed()
    }

    @Test
    fun `the one you are in is marked`() {
        render(current = "lib-2")

        // The card merges its children's semantics, so the marker is only a node
        // of its own in the unmerged tree.
        compose.onNodeWithTag("library_lib-2_current", useUnmergedTree = true).assertExists()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `picking one reports it`() {
        var chosen: Library? = null
        render(onSelect = { chosen = it })

        compose.onNodeWithTag("library_lib-2").performSemanticsAction(SemanticsActions.RequestFocus)
        compose.onNodeWithTag("library_lib-2").performKeyInput { pressKey(Key.DirectionCenter) }
        compose.waitForIdle()

        assertEquals("lib-2", chosen?.id)
    }

    @Test
    fun `a server with no libraries says so rather than showing nothing`() {
        compose.setContent {
            FahrenheitTheme {
                SwitchLibraryView(libraries = emptyList(), currentId = null, onSelect = {})
            }
        }

        compose.onNodeWithText("No libraries", substring = true).assertIsDisplayed()
    }
}
