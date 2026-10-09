package com.paulohenriquesg.fahrenheit.settings

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.onNodeWithText
import kotlinx.coroutines.CompletableDeferred
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import com.paulohenriquesg.fahrenheit.api.Playlist
import com.paulohenriquesg.fahrenheit.api.PlaylistItem
import com.paulohenriquesg.fahrenheit.favourites.FAVOURITES_CREATE_TAG
import com.paulohenriquesg.fahrenheit.favourites.FAVOURITES_NONE_TAG
import com.paulohenriquesg.fahrenheit.favourites.FAVOURITES_ROW_TAG
import com.paulohenriquesg.fahrenheit.favourites.FAVOURITES_SEARCH_TAG
import com.paulohenriquesg.fahrenheit.favourites.FavouritesPick
import com.paulohenriquesg.fahrenheit.favourites.FavouritesSetting
import com.paulohenriquesg.fahrenheit.favourites.favouritesOptionTag
import com.paulohenriquesg.fahrenheit.player.SIDE_PANEL_SCRIM_TAG
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import com.paulohenriquesg.fahrenheit.ui.theme.ThemePreference
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowToast
import java.io.IOException

/** Settings → Playback → Favourites playlist (#180; frame 0 of docs/mocks/podcast-actions.html). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w960dp-h540dp")
class FavouritesSettingTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val picks = mutableListOf<FavouritesPick>()

    private fun playlist(id: String, name: String, count: Int = 0) =
        Playlist(id, name, "lib_1", List(count) { PlaylistItem("li_$it") })

    private fun render(
        chosen: Playlist? = null,
        playlists: List<Playlist> = listOf(playlist("pl_1", "Bedtime", 9), playlist("pl_2", "Kids", 6)),
        choose: Result<Unit> = Result.success(Unit),
        favourites: Boolean = true,
        libraryName: String = "Podcasts"
    ) {
        val setting = FavouritesSetting(
            libraryId = "lib_1",
            libraryName = libraryName,
            chosen = chosen,
            load = { Result.success(playlists) },
            choose = { picks += it; choose }
        )
        compose.setContent {
            FahrenheitTheme {
                SettingsView(
                    theme = ThemePreference.System, onTheme = {}, rowLayout = true, onLayout = {},
                    version = "v0.0.10", update = UpdateCheck.Idle, onCheckUpdates = {}, onInstall = {},
                    username = "a listener", server = "http://books.example:13378", onSignOut = {},
                    deviceName = "Living room", onDeviceName = {},
                    favourites = setting.takeIf { favourites }
                )
            }
        }
        compose.waitForIdle()
    }

    private fun open() {
        compose.onNodeWithTag(FAVOURITES_ROW_TAG).performScrollTo().performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
    }

    @Test
    fun `the row names the library and says None until one is chosen`() {
        render()

        compose.onNodeWithText("Favourites playlist · Podcasts").performScrollTo()
        compose.onNodeWithTag(FAVOURITES_ROW_TAG).performScrollTo()
        compose.onNodeWithText("None ›").assertExists()
    }

    @Test
    fun `the row shows the playlist chosen`() {
        render(chosen = playlist("pl_1", "Bedtime", 9))

        compose.onNodeWithText("Bedtime ›").assertExists()
    }

    @Test
    fun `no library, no row`() {
        render(favourites = false)

        compose.onNodeWithTag(FAVOURITES_ROW_TAG).assertDoesNotExist()
    }

    @Test
    fun `Center opens a panel with None, Create and the playlists with their counts`() {
        render()
        open()

        compose.onNodeWithTag(SIDE_PANEL_SCRIM_TAG).assertExists()
        compose.onNodeWithTag(FAVOURITES_NONE_TAG).assertExists()
        compose.onNodeWithTag(FAVOURITES_CREATE_TAG).assertExists()
        compose.onNodeWithText("Bedtime").assertExists()
        compose.onNodeWithText("9 items").assertExists()
        compose.onNodeWithText("Kids").assertExists()
    }

    @Test
    fun `Create is not offered beside a playlist already called Favourites`() {
        render(playlists = listOf(playlist("pl_1", "Favourites", 48)))
        open()

        compose.onNodeWithTag(FAVOURITES_CREATE_TAG).assertDoesNotExist()
        compose.onNodeWithTag(favouritesOptionTag("pl_1")).assertExists()
    }

    @Test
    fun `focus opens on None when nothing is chosen`() {
        render()
        open()

        compose.onNodeWithTag(FAVOURITES_NONE_TAG).assertIsFocused()
    }

    @Test
    fun `focus opens on the current choice`() {
        render(chosen = playlist("pl_2", "Kids", 6))
        open()

        compose.onNodeWithTag(favouritesOptionTag("pl_2")).assertIsFocused()
    }

    @Test
    fun `Center on a playlist picks it and closes the panel`() {
        render()
        open()

        compose.onNodeWithTag(favouritesOptionTag("pl_1")).performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()

        assertEquals("pl_1", (picks.single() as FavouritesPick.Existing).playlist.id)
        compose.onNodeWithTag(SIDE_PANEL_SCRIM_TAG).assertDoesNotExist()
    }

    @Test
    fun `Create and None are picks too`() {
        render()
        open()
        compose.onNodeWithTag(FAVOURITES_CREATE_TAG).performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        open()
        compose.onNodeWithTag(FAVOURITES_NONE_TAG).performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()

        assertEquals(listOf(FavouritesPick.Create, FavouritesPick.None), picks)
    }

    @Test
    fun `a pick that fails says so and keeps the panel open`() {
        render(choose = Result.failure(IOException("offline")))
        open()

        compose.onNodeWithTag(FAVOURITES_CREATE_TAG).performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()

        compose.onNodeWithTag(SIDE_PANEL_SCRIM_TAG).assertExists()
        assertEquals("Couldn't change Favourites. Check the connection and try again.", ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun `up to twelve playlists, no search field`() {
        render(playlists = List(12) { playlist("pl_$it", "List $it") })
        open()

        compose.onNodeWithTag(FAVOURITES_SEARCH_TAG).assertDoesNotExist()
    }

    @Test
    fun `more than twelve, a search field narrows them as you type`() {
        render(playlists = List(13) { playlist("pl_$it", "List $it") } + playlist("pl_x", "Bedtime"))
        open()

        compose.onNodeWithTag(FAVOURITES_SEARCH_TAG).performTextReplacement("bed")
        compose.waitForIdle()

        compose.onNodeWithTag(favouritesOptionTag("pl_x")).assertExists()
        compose.onNodeWithTag(favouritesOptionTag("pl_3")).assertDoesNotExist()
    }

    @Test
    fun `focus opens on the current choice far down a long list`() {
        val many = List(31) { playlist("pl_$it", "List ${it + 10}") }
        render(chosen = many[25], playlists = many)
        open()

        compose.onNodeWithTag(favouritesOptionTag("pl_25")).assertIsFocused()
    }

    // MainScreen hands the panel a new setting as soon as a pick is kept; the
    // panel went back to "Loading" and asked for the playlists again.
    @Test
    fun `a pick being kept does not send the panel back to loading`() {
        var loads = 0
        val held = CompletableDeferred<Result<Unit>>()
        val playlists = listOf(playlist("pl_1", "Bedtime", 9))
        fun setting(chosen: Playlist?, onChoose: suspend (FavouritesPick) -> Result<Unit>): FavouritesSetting =
            FavouritesSetting("lib_1", "Podcasts", chosen, load = { loads++; Result.success(playlists) }, choose = onChoose)
        var current by mutableStateOf<FavouritesSetting?>(null)
        current = setting(null) { pick ->
            current = setting((pick as FavouritesPick.Existing).playlist) { Result.success(Unit) }
            held.await()
        }
        compose.setContent {
            FahrenheitTheme {
                SettingsView(
                    theme = ThemePreference.System, onTheme = {}, rowLayout = true, onLayout = {},
                    version = "v0.0.10", update = UpdateCheck.Idle, onCheckUpdates = {}, onInstall = {},
                    username = "a listener", server = "http://books.example:13378", onSignOut = {},
                    deviceName = "Living room", onDeviceName = {},
                    favourites = current
                )
            }
        }
        compose.waitForIdle()
        open()

        compose.onNodeWithTag(favouritesOptionTag("pl_1")).performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()

        compose.onNodeWithText("Loading playlists…").assertDoesNotExist()
        compose.onNodeWithTag(favouritesOptionTag("pl_1")).assertExists()
        assertEquals(1, loads)
    }

    @Test
    fun `while the playlists load, the panel does not count them as none`() {
        val setting = FavouritesSetting("lib_1", "Podcasts", null, load = { CompletableDeferred<Result<List<Playlist>>>().await() }, choose = { Result.success(Unit) })
        compose.setContent {
            FahrenheitTheme {
                SettingsView(
                    theme = ThemePreference.System, onTheme = {}, rowLayout = true, onLayout = {},
                    version = "v0.0.10", update = UpdateCheck.Idle, onCheckUpdates = {}, onInstall = {},
                    username = "a listener", server = "http://books.example:13378", onSignOut = {},
                    deviceName = "Living room", onDeviceName = {},
                    favourites = setting
                )
            }
        }
        compose.waitForIdle()
        open()

        compose.onNodeWithText("Loading playlists…").assertExists()
        compose.onNodeWithText("Podcasts · 0 playlists").assertDoesNotExist()
        compose.onNodeWithText("Podcasts").assertExists()
    }

    @Test
    fun `a long library name stays on one line`() {
        render(libraryName = "An Invented Library " .repeat(12).trim())

        val title = compose.onNodeWithText("Favourites playlist", substring = true).performScrollTo().fetchSemanticsNode()
        val layouts = mutableListOf<TextLayoutResult>()
        title.config[androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
        assertEquals(1, layouts.single().lineCount)
    }
}
