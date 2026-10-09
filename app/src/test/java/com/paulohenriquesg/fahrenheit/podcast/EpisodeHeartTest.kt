package com.paulohenriquesg.fahrenheit.podcast

import androidx.activity.ComponentActivity
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.paulohenriquesg.fahrenheit.api.Episode
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The heart on the focused episode row, after Mark finished, and the
 * Favourites tab (#180; frame 2 of docs/mocks/podcast-actions.html).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w960dp-h540dp")
class EpisodeHeartTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val gson = Gson()
    private val onServer: Episode = gson.fromJson(
        """{"libraryItemId":"li","id":"s1","index":1,"title":"Tabstack","publishedAt":3,"addedAt":0,"updatedAt":0,"guid":"g1"}""",
        Episode::class.java
    )
    private fun feed(guid: String, title: String, at: Long): JsonObject = gson.fromJson(
        """{"title":"$title","guid":"$guid","publishedAt":$at,"enclosure":{"url":"https://cdn/$guid.mp3"}}""",
        JsonObject::class.java
    )

    private var toggled: Episode? = null
    private var played: Episode? = null
    private var tabChosen: EpisodeTab? = null

    private fun render(favourites: Set<String>? = emptySet(), playlist: String = "Bedtime") {
        val screen = PodcastScreenModel.of(
            listOf(onServer), FeedLoad.Loaded(listOf(feed("g1", "Tabstack", 3), feed("g2", "Supergood", 2))),
            EpisodeTab.All, null, false, now = 10, favourites = favourites
        )
        compose.setContent {
            FahrenheitTheme {
                PodcastEpisodesView(
                    screen = screen,
                    tab = EpisodeTab.All,
                    onTab = { tabChosen = it },
                    downloads = emptyMap(),
                    onPlay = { played = it },
                    onDownload = {},
                    hearts = favourites?.let { EpisodeHearts(playlist, it, onToggle = { episode -> toggled = episode }) }
                )
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun `the Favourites tab carries its count, and a press chooses it`() {
        render(favourites = setOf("s1"))

        compose.onNodeWithText("Favourites · 1").assertExists()
        compose.onNodeWithTag("episode_tab_Favourites").performSemanticsAction(SemanticsActions.OnClick)

        assertEquals(EpisodeTab.Favourites, tabChosen)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `Right, Right from the focused row reaches the heart, and Left goes back to Mark finished`() {
        render()
        compose.onNodeWithTag("episode_row_server:s1").performKeyInput { pressKey(Key.DirectionRight) }
        compose.waitForIdle()
        compose.onNodeWithTag("episode_mark_server:s1").performKeyInput { pressKey(Key.DirectionRight) }
        compose.waitForIdle()

        compose.onNodeWithTag("episode_favourite_server:s1").assertIsFocused()

        compose.onNodeWithTag("episode_favourite_server:s1").performKeyInput { pressKey(Key.DirectionLeft) }
        compose.waitForIdle()
        compose.onNodeWithTag("episode_mark_server:s1").assertIsFocused()
    }

    @Test
    fun `it is round, the size of Mark finished`() {
        render()

        compose.onNodeWithTag("episode_favourite_server:s1").assertWidthIsEqualTo(40.dp).assertHeightIsEqualTo(40.dp)
    }

    @Test
    fun `not in, it offers to favourite`() {
        render()

        compose.onNodeWithTag("episode_favourite_server:s1").assertContentDescriptionEquals("Favourite")
    }

    @Test
    fun `in, it offers to take it out of the playlist`() {
        render(favourites = setOf("s1"))

        compose.onNodeWithTag("episode_favourite_server:s1").assertContentDescriptionEquals("Remove from Bedtime")
    }

    @Test
    fun `a press asks for that episode, and the row does not play`() {
        render()

        compose.onNodeWithTag("episode_favourite_server:s1").performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()

        assertEquals("s1", toggled?.id)
        assertNull(played)
    }

    @Test
    fun `with None chosen there is no heart and no tab`() {
        render(favourites = null)

        compose.onNodeWithTag("episode_mark_server:s1").assertExists()
        compose.onNodeWithTag("episode_favourite_server:s1").assertDoesNotExist()
        compose.onNodeWithTag("episode_tab_Favourites").assertDoesNotExist()
    }

    @Test
    fun `only the focused row has it, and never one the server lacks`() {
        render()
        compose.onNodeWithTag("episode_row_feed:g2").performSemanticsAction(SemanticsActions.RequestFocus)
        compose.waitForIdle()

        compose.onNodeWithTag("episode_favourite_server:s1").assertDoesNotExist()
        compose.onNodeWithTag("episode_favourite_feed:g2").assertDoesNotExist()
    }
}
