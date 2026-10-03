package com.paulohenriquesg.fahrenheit.library

import androidx.activity.ComponentActivity
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import com.paulohenriquesg.fahrenheit.api.Library
import com.paulohenriquesg.fahrenheit.api.LibraryStats
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

    private val stats = mapOf(
        "lib-1" to LibraryStats(totalItems = 38, totalDuration = 181 * 3600.0 + 1000, numAudioTracks = 412),
        "lib-2" to LibraryStats(totalItems = 12, totalDuration = 90_000.0, numAudioTracks = 340)
    )

    private fun render(
        current: String? = "lib-1",
        stats: Map<String, LibraryStats> = this.stats,
        onSelect: (Library) -> Unit = {}
    ) {
        compose.setContent {
            FahrenheitTheme {
                SwitchLibraryView(libraries = libraries, currentId = current, stats = stats, onSelect = onSelect)
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

    @Test
    fun `a book library says how many books and how many hours`() {
        render()

        compose.onNodeWithText("38 books \u00b7 181 hours", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `a podcast library says how many shows and episodes`() {
        render()

        compose.onNodeWithText("12 shows \u00b7 340 episodes", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `one of each reads as one, not as ones`() {
        render(stats = mapOf(
            "lib-1" to LibraryStats(totalItems = 1, totalDuration = 3600.0, numAudioTracks = 1),
            "lib-2" to LibraryStats(totalItems = 1, totalDuration = 60.0, numAudioTracks = 1)
        ))

        compose.onNodeWithText("1 book \u00b7 1 hour", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("1 show \u00b7 1 episode", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `a short library does not round down to no time at all`() {
        render(stats = mapOf("lib-1" to LibraryStats(totalItems = 2, totalDuration = 1500.0, numAudioTracks = 2)))

        compose.onNodeWithText("2 books \u00b7 25 minutes", useUnmergedTree = true).assertIsDisplayed()
    }

    // Until the counts arrive, or if they never do, say what kind of library it is.
    @Test
    fun `without counts a tile still says what it holds`() {
        render(stats = emptyMap())

        compose.onNodeWithText("Book library", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("Podcast library", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `adding a library is said to happen on the server, and is not a button`() {
        render()

        compose.onNodeWithText("On the server, not here", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("library_add")
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnClick))
    }
}
