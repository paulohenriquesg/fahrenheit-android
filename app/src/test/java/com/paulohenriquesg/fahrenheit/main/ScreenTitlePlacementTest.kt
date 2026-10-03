package com.paulohenriquesg.fahrenheit.main

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.paulohenriquesg.fahrenheit.api.ListeningStatsResponse
import com.paulohenriquesg.fahrenheit.library.SwitchLibraryView
import com.paulohenriquesg.fahrenheit.podcast.LatestEpisodesView
import com.paulohenriquesg.fahrenheit.settings.SettingsView
import com.paulohenriquesg.fahrenheit.settings.UpdateCheck
import com.paulohenriquesg.fahrenheit.ui.Space
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import com.paulohenriquesg.fahrenheit.ui.theme.ThemePreference
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Every section reached from the rail puts its title in the same place, in every
 * state, so switching sections does not move it (#82): Space.screenH in from the
 * content edge and Space.gap down, on the band the search icon and greeting share.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w960dp-h540dp")
class ScreenTitlePlacementTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun render(content: @Composable () -> Unit) {
        compose.setContent { FahrenheitTheme { content() } }
        compose.waitForIdle()
    }

    private fun assertTitleInPlace(title: String) {
        val bounds = compose.onNodeWithText(title).assertIsDisplayed().getUnclippedBoundsInRoot()
        assertEquals("start of \"$title\"", Space.screenH.value, bounds.left.value, 0.5f)
        assertEquals("top of \"$title\"", Space.gap.value, bounds.top.value, 0.5f)
    }

    /** Short enough that the screen has to scroll, as on a 1080p TV at density 2. */
    @Composable
    private fun Short(content: @Composable () -> Unit) {
        Box(modifier = Modifier.fillMaxWidth().requiredHeight(300.dp)) { content() }
    }

    private val stats = ListeningStatsResponse(
        totalTime = 651_600.0,
        items = emptyMap(),
        days = mapOf("2026-09-30" to 600.0),
        dayOfWeek = mapOf("Tuesday" to 600.0),
        today = 0.0
    )

    @Test
    fun `Home has its title while it loads`() {
        render { PersonalizedHomeView(shelves = emptyList(), libraryId = "lib", isLoading = true) }
        assertTitleInPlace("Home")
    }

    @Test
    fun `Home has its title once loaded`() {
        render { PersonalizedHomeView(shelves = emptyList(), libraryId = "lib", isLoading = false) }
        assertTitleInPlace("Home")
    }

    @Test
    fun `Series has its title while it loads`() {
        render { SeriesBrowseView(seriesList = emptyList(), isLoading = true) }
        assertTitleInPlace("Series")
    }

    @Test
    fun `Series has its title when there are none`() {
        render { SeriesBrowseView(seriesList = emptyList(), isLoading = false) }
        assertTitleInPlace("Series")
    }

    @Test
    fun `Authors has its title`() {
        render { AuthorsBrowseView(libraryId = null) }
        assertTitleInPlace("Authors")
    }

    @Test
    fun `Collections has its title while loading`() {
        render { CollectionsBrowseView(collectionsList = emptyList(), isLoading = true) }
        assertTitleInPlace("Collections")
    }

    @Test
    fun `Collections has its title when there are none`() {
        render { CollectionsBrowseView(collectionsList = emptyList(), isLoading = false) }
        assertTitleInPlace("Collections")
    }

    @Test
    fun `Stats has its title while it loads`() {
        render { StatsBrowseView(stats = null, isLoading = true) }
        assertTitleInPlace("Listening Statistics")
    }

    @Test
    fun `Stats has its title when there are none`() {
        render { StatsBrowseView(stats = null, isLoading = false) }
        assertTitleInPlace("Listening Statistics")
    }

    @Test
    fun `the Stats title stays put when the board scrolls`() {
        render { Short { StatsBrowseView(stats = stats, isLoading = false) } }

        compose.onNodeWithText("Recently listened").performScrollTo()
        compose.waitForIdle()

        assertTitleInPlace("Listening Statistics")
    }

    @Test
    fun `Latest Episodes has its title`() {
        render { LatestEpisodesView(libraryId = "lib") }
        assertTitleInPlace("Latest Episodes")
    }

    @Test
    fun `the Settings title stays put when the settings scroll`() {
        render {
            Short {
                SettingsView(
                    theme = ThemePreference.System,
                    onTheme = {},
                    rowLayout = true,
                    onLayout = {},
                    version = "v0.0.10",
                    update = UpdateCheck.Idle,
                    onCheckUpdates = {},
                    username = "admin",
                    server = "http://books.example:13378",
                    onSignOut = {},
                    deviceName = "AFTMODEL",
                    onDeviceName = {}
                )
            }
        }

        compose.onNodeWithTag("sign_out").performScrollTo()
        compose.waitForIdle()

        assertTitleInPlace("Settings")
    }

    @Test
    fun `Switch Library has its title`() {
        render { SwitchLibraryView(libraries = emptyList(), currentId = null, onSelect = {}) }
        assertTitleInPlace("Switch library")
    }

    @Test
    fun `Library has its title, with the count beside it`() {
        render {
            LibraryBrowseView(
                name = "Audiobooks",
                itemLabel = "books",
                items = emptyList(),
                rowLayout = true,
                listState = androidx.compose.foundation.lazy.rememberLazyListState()
            )
        }
        assertTitleInPlace("Audiobooks")
        compose.onNodeWithText("(0 books)").assertIsDisplayed()
    }

    @Test
    fun `Library has a title before a library is chosen`() {
        render {
            LibraryBrowseView(
                name = null,
                itemLabel = "books",
                items = emptyList(),
                rowLayout = false,
                listState = androidx.compose.foundation.lazy.rememberLazyListState()
            )
        }
        assertTitleInPlace("Library")
    }
}
