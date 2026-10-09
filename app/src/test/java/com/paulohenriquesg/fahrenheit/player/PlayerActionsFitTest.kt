package com.paulohenriquesg.fahrenheit.player

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.TextLayoutResult
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.test.utils.TestExoPlayerBuilder
import com.paulohenriquesg.fahrenheit.favourites.FavouriteButton
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.ceil

/**
 * Every action beside the transport keeps its own width on a 1920×1080 TV
 * (#180): with the heart added, About was squeezed to a sliver on a device.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w960dp-h540dp-xhdpi")
class PlayerActionsFitTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private lateinit var player: ExoPlayer

    @After
    fun tearDown() = player.release()

    private val oneFile = TrackTimeline(listOf(TimelineTrack(index = 1, startOffset = 0.0, duration = 1800.0, contentUrl = "/x")))

    private fun show(
        nowPlaying: NowPlaying,
        episodes: EpisodeSkip?,
        chapters: Boolean,
        goToPodcast: Boolean,
        speed: Float = 1.25f,
        // The longest the sleep chip reads.
        sleep: SleepState? = SleepState(SleepChoice.Minutes(120), minutesLeft = 120)
    ) {
        player = TestExoPlayerBuilder(compose.activity).setMediaSourceFactory(hourLongFiles()).build()
        compose.setContent {
            FahrenheitTheme {
                val panels = rememberPlayerPanels()
                PlayerScreen(nowPlaying = nowPlaying, currentTime = 700.0, transport = {
                    MediaPlayerController(
                        player = player, playback = BookPlayback(player, oneFile), totalTime = oneFile.totalDuration,
                        chapters = nowPlaying.chapters, episodes = episodes,
                        trailing = {
                            PlayerActions(
                                panels = panels,
                                speed = speed,
                                sleep = sleep,
                                chapters = chapters,
                                onGoToPodcast = if (goToPodcast) ({}) else null,
                                favourite = { FavouriteButton(filled = true, onClick = {}) }
                            )
                        }
                    )
                })
            }
        }
        compose.waitForIdle()
    }

    private fun layout(node: SemanticsNode): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        node.config[SemanticsActions.GetTextLayoutResult].action!!.invoke(results)
        return results.single()
    }

    /** Each label on one line, as wide as it wants, and on the screen. */
    private fun assertFits(vararg labels: String) {
        val right = compose.onRoot().fetchSemanticsNode().boundsInRoot.right
        labels.forEach { label ->
            val node = compose.onAllNodesWithText(label).fetchSemanticsNodes().single()
            val text = layout(node)
            assertEquals("$label wraps", 1, text.lineCount)
            assertTrue(
                "$label is squeezed to ${text.size.width}px",
                text.size.width >= ceil(text.multiParagraph.intrinsics.maxIntrinsicWidth).toInt()
            )
            assertTrue("$label runs off the screen", node.boundsInRoot.right <= right)
        }
    }

    @Test
    fun `an episode's actions all fit beside the transport`() {
        show(
            NowPlaying("p1", "An Episode", oneFile, null, null, "e1", goToPodcast = true, description = null),
            episodes = EpisodeSkip(onPrevious = {}, onNext = {}),
            chapters = false,
            goToPodcast = true
        )

        assertFits("Go to podcast", "Speed 1.25×", "Sleep 120 min", "About")
    }

    @Test
    fun `at 1x with no timer, an episode's actions stay beside the transport`() {
        show(
            NowPlaying("p1", "An Episode", oneFile, null, null, "e1", goToPodcast = true, description = null),
            episodes = EpisodeSkip(onPrevious = {}, onNext = {}),
            chapters = false,
            goToPodcast = true,
            speed = 1f,
            sleep = null
        )

        assertFits("Go to podcast", "Speed 1×", "Sleep", "About")
        val play = compose.onNodeWithContentDescription("Play").fetchSemanticsNode().boundsInRoot
        val about = compose.onAllNodesWithText("About").fetchSemanticsNodes().single().boundsInRoot
        assertEquals(play.center.y, about.center.y, 1f)
    }

    @Test
    fun `a book's actions all fit beside the transport`() {
        show(
            NowPlaying(
                "b1", "A Book", oneFile, null,
                chapters = listOf(
                    com.paulohenriquesg.fahrenheit.api.Chapter(start = 0.0, end = 600.0, title = "One"),
                    com.paulohenriquesg.fahrenheit.api.Chapter(start = 600.0, end = 1800.0, title = "Two")
                ),
                episodeId = null, goToPodcast = false, description = null
            ),
            episodes = null,
            chapters = true,
            goToPodcast = false
        )

        assertFits("Chapters", "Speed 1.25×", "Sleep 120 min", "About")
    }
}
