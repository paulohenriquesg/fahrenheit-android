package com.paulohenriquesg.fahrenheit.player

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.activity.ComponentActivity
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.tv.material3.Text
import com.paulohenriquesg.fahrenheit.R
import com.paulohenriquesg.fahrenheit.api.Chapter
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.performSemanticsAction
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.test.utils.TestExoPlayerBuilder
import androidx.media3.test.utils.robolectric.TestPlayerRunHelper.run
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The transport works the player it is given, in whole-book time (#16).
 *
 * Buttons are pressed through their click action: they are TV buttons, which
 * act on keys and semantics, not on injected touch.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w960dp-h540dp")
class TransportTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private lateinit var player: ExoPlayer

    private val twoParts = TrackTimeline(
        listOf(
            TimelineTrack(index = 1, startOffset = 0.0, duration = 3600.0, contentUrl = "/part1"),
            TimelineTrack(index = 2, startOffset = 3600.0, duration = 1800.0, contentUrl = "/part2")
        )
    )

    @After
    fun tearDown() = player.release()

    private fun show(
        player: ExoPlayer,
        timeline: TrackTimeline = twoParts,
        chapters: List<Chapter>? = null,
        onPlay: () -> Unit = { player.play() },
        focusPlayAgain: () -> Int = { 0 }
    ) {
        this.player = player
        compose.setContent {
            FahrenheitTheme {
                MediaPlayerController(
                    player = player, playback = BookPlayback(player, timeline), totalTime = timeline.totalDuration,
                    chapters = chapters, onPlay = onPlay, focusPlayAgain = focusPlayAgain()
                )
            }
        }
        compose.waitForIdle()
    }

    /** Queued but not prepared: an unprepared player keeps the position it is given, exactly. */
    private fun queuedAt(startAt: Double): ExoPlayer {
        val p = TestExoPlayerBuilder(compose.activity).setMediaSourceFactory(hourLongFiles()).build()
        val nowPlaying = NowPlaying("b1", "t", twoParts, null, null, null, false, null)
        val queue = PlaybackQueue.of(nowPlaying, startAt) { "https://abs.test$it" }!!
        p.setMediaItems(queue.items, queue.index, queue.positionMs)
        return p
    }

    @Test
    fun `play plays and pause pauses`() {
        show(queuedAt(0.0))

        compose.onNodeWithContentDescription("Play").performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        assertTrue(player.playWhenReady)

        compose.onNodeWithContentDescription("Pause").performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        assertFalse(player.playWhenReady)
    }

    // #90: Play from a pause asks first whether to continue from elsewhere.
    @Test
    fun `play from a pause goes through onPlay`() {
        var asked = 0
        show(queuedAt(0.0), onPlay = { asked++ })

        compose.onNodeWithContentDescription("Play").performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()

        assertEquals(1, asked)
        assertFalse(player.playWhenReady)
    }

    // After the question closes, its focused button is gone; Play takes focus.
    @Test
    fun `asked to, Play takes focus again`() {
        var again by androidx.compose.runtime.mutableIntStateOf(0)
        show(queuedAt(0.0), focusPlayAgain = { again })
        compose.onNodeWithContentDescription(compose.activity.getString(com.paulohenriquesg.fahrenheit.R.string.skip_forward_seconds, 30))
            .performSemanticsAction(SemanticsActions.RequestFocus)

        again++
        compose.waitForIdle()

        compose.onNodeWithContentDescription("Play").assertIsFocused()
    }

    @Test
    fun `pausing does not go through onPlay`() {
        var asked = 0
        show(queuedAt(0.0).apply { play() }, onPlay = { asked++ })

        compose.onNodeWithContentDescription("Pause").performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()

        assertEquals(0, asked)
        assertFalse(player.playWhenReady)
    }

    @Test
    fun `skipping forward near the end of a file crosses into the next`() {
        show(queuedAt(3590.0))

        compose.onNodeWithContentDescription(compose.activity.getString(com.paulohenriquesg.fahrenheit.R.string.skip_forward_seconds, 30)).performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()

        assertEquals(1, player.currentMediaItemIndex)
        assertEquals(20_000L, player.currentPosition)
    }

    @Test
    fun `the times are whole-book times, with what is left`() {
        show(queuedAt(3900.0))

        compose.onNodeWithText("1 h 5 min").assertIsDisplayed()
        compose.onNodeWithText("25 min 0 s left of 1 h 30 min").assertIsDisplayed()
    }

    // Frame 4 has three focus stops. Stop only paused, as Play/Pause does;
    // ending playback is Stop on the rail's Now playing entry (#93, #155).
    @Test
    fun `there is no stop button`() {
        show(queuedAt(0.0))

        compose.onNodeWithContentDescription("Stop").assertDoesNotExist()
    }

    // Review Focus 5.
    @Test
    fun `after an error, play retries where it was`() {
        // The real media source: an unresolvable host fails to load.
        val failing = TestExoPlayerBuilder(compose.activity).build()
        failing.setMediaItems(
            listOf(MediaItem.fromUri("http://abs.invalid/1.mp3"), MediaItem.fromUri("http://abs.invalid/2.mp3")),
            1, 300_000L
        )
        failing.prepare()
        run(failing).untilPlayerError()
        show(failing)

        compose.onNodeWithText("Couldn't play this").assertIsDisplayed()
        compose.onNodeWithContentDescription("Play").performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()

        assertNull(player.playerError)
        assertEquals(1, player.currentMediaItemIndex)
        assertEquals(300_000L, player.currentPosition)
        assertTrue(player.playbackState != Player.STATE_IDLE)
    }

    private val chapters = listOf(
        Chapter(start = 0.0, end = 1800.0, title = "One"),
        Chapter(start = 1800.0, end = 3600.0, title = "Two"),
        Chapter(start = 3600.0, end = 5400.0, title = "Three")
    )

    private fun press(description: String) =
        compose.onNodeWithContentDescription(description).performSemanticsAction(SemanticsActions.OnClick)

    @Test
    fun `with chapters, the first bar is the chapter and the second the book`() {
        show(queuedAt(3900.0), chapters = chapters)

        compose.onNodeWithText("5 min 0 s").assertIsDisplayed()
        compose.onNodeWithText("25 min 0 s left in chapter").assertIsDisplayed()
        compose.onNodeWithText("1 h 5 min of 1 h 30 min").assertIsDisplayed()
        compose.onNodeWithText("25 min 0 s left").assertIsDisplayed()
    }

    @Test
    fun `next chapter goes to the next chapter's start`() {
        show(queuedAt(1900.0), chapters = chapters)

        press(compose.activity.getString(R.string.next_chapter))
        compose.waitForIdle()

        assertEquals(1, player.currentMediaItemIndex)
        assertEquals(0L, player.currentPosition)
    }

    @Test
    fun `previous chapter restarts the chapter that is playing`() {
        show(queuedAt(1810.0), chapters = chapters)

        press(compose.activity.getString(R.string.previous_chapter))
        compose.waitForIdle()

        assertEquals(1_800_000L, player.currentPosition)
    }

    @Test
    fun `previous chapter right after a start goes to the one before`() {
        show(queuedAt(1801.0), chapters = chapters)

        press(compose.activity.getString(R.string.previous_chapter))
        compose.waitForIdle()

        assertEquals(0L, player.currentPosition)
    }

    // Review Focus 1.
    @Test
    fun `without chapters there is one bar and no chapter buttons`() {
        show(queuedAt(0.0))

        compose.onNodeWithContentDescription(compose.activity.getString(R.string.next_chapter)).assertDoesNotExist()
        compose.onNodeWithTag(BOOK_BAR_TAG).assertDoesNotExist()
        compose.onNodeWithTag(CHAPTER_BAR_TAG).assertExists()
    }

    // Review Focus 4.
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `up from play reaches the chapter bar, which seeks`() {
        show(queuedAt(1900.0), chapters = chapters)

        compose.onNodeWithContentDescription("Play").performKeyInput { pressKey(Key.DirectionUp) }
        compose.onNodeWithTag(CHAPTER_BAR_TAG).assertIsFocused()
        compose.onNodeWithTag(CHAPTER_BAR_TAG).performKeyInput { pressKey(Key.DirectionRight) }
        compose.waitForIdle()

        assertEquals(1_910_000L, player.currentPosition)
    }

    @Test
    fun `actions sit beside the transport`() {
        player = queuedAt(0.0)
        compose.setContent {
            FahrenheitTheme {
                MediaPlayerController(player, BookPlayback(player, twoParts), twoParts.totalDuration, trailing = { Text("ACTIONS") })
            }
        }

        compose.onNodeWithText("ACTIONS").assertIsDisplayed()
    }

    // Review: chapter skip read the position the screen last polled, up to a
    // second old while playing, so the 3 s restart rule misfired.
    @Test
    fun `chapter skip reads where the player is now, not the last poll`() {
        show(queuedAt(1801.0), chapters = chapters)
        // The player moves on without the screen polling (as between polls).
        player.seekTo(0, 1_810_000L)

        press(compose.activity.getString(R.string.previous_chapter))
        compose.waitForIdle()

        assertEquals(1_800_000L, player.currentPosition)
    }

    // Review: Go to podcast moved beside the transport; it must be reachable by key.
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `right from the transport reaches the actions`() {
        player = queuedAt(0.0)
        compose.setContent {
            FahrenheitTheme {
                MediaPlayerController(player, BookPlayback(player, twoParts), twoParts.totalDuration, trailing = { GoToPodcastButton {} })
            }
        }
        compose.waitForIdle()

        compose.onNodeWithContentDescription("Play").performKeyInput { pressKey(Key.DirectionRight) }
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.skip_forward_seconds, 30)).performKeyInput { pressKey(Key.DirectionRight) }

        compose.onNodeWithTag(GO_TO_PODCAST_TAG).assertIsFocused()
    }

    @Test
    fun `at a speed, the book's time left counts at it`() {
        val p = queuedAt(3900.0).apply { setPlaybackSpeed(1.25f) }
        show(p, chapters = chapters)

        // One meaning of "left": real listening time, in the chapter as in the book.
        compose.onNodeWithText("20 min 0 s left in chapter").assertIsDisplayed()
        compose.onNodeWithText("20 min 0 s left at 1.25×").assertIsDisplayed()
    }

    @Test
    fun `without chapters, the one row counts at the speed too`() {
        show(queuedAt(0.0).apply { setPlaybackSpeed(1.5f) })

        compose.onNodeWithText("1 h 0 min left of 1 h 30 min at 1.5×").assertIsDisplayed()
    }

    @Test
    fun `a new speed changes the time left at once`() {
        show(queuedAt(3900.0), chapters = chapters)

        compose.runOnUiThread { player.setPlaybackSpeed(2f) }
        compose.waitForIdle()

        compose.onNodeWithText("12 min 30 s left at 2×").assertIsDisplayed()
        compose.onNodeWithText("12 min 30 s left in chapter").assertIsDisplayed()
    }

    // The Chapters panel seeks from outside the transport, often while paused.
    @Test
    fun `a seek from elsewhere moves the times at once, even paused`() {
        show(queuedAt(0.0), chapters = chapters)

        compose.runOnUiThread { BookPlayback(player, twoParts).seekToBookTime(3900.0) }
        compose.waitForIdle()

        compose.onNodeWithText("25 min 0 s left in chapter").assertIsDisplayed()
    }

    private fun showEpisode(previous: (() -> Unit)?, next: (() -> Unit)?) {
        player = queuedAt(0.0)
        compose.setContent {
            FahrenheitTheme {
                MediaPlayerController(
                    player = player, playback = BookPlayback(player, twoParts), totalTime = twoParts.totalDuration,
                    episodes = EpisodeSkip(onPrevious = previous, onNext = next)
                )
            }
        }
        compose.waitForIdle()
    }

    // #108: an episode's outer buttons go to the episodes either side.
    @Test
    fun `an episode's outer buttons go to the previous and next episode`() {
        var went = ""
        showEpisode(previous = { went += "<" }, next = { went += ">" })

        press(compose.activity.getString(R.string.next_episode))
        press(compose.activity.getString(R.string.previous_episode))

        assertEquals("><", went)
    }

    @Test
    fun `the newest episode's Next is there, but off`() {
        showEpisode(previous = {}, next = null)

        compose.onNodeWithContentDescription(compose.activity.getString(R.string.next_episode)).assertIsNotEnabled()
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.previous_episode)).assertIsEnabled()
    }

    // Review Focus 4.
    @Test
    fun `a book has no episode buttons`() {
        show(queuedAt(1900.0), chapters = chapters)
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.next_episode)).assertDoesNotExist()
    }

    // #107: skip lengths from Settings.
    private fun showSkipping(back: Int, forward: Int) {
        player = queuedAt(600.0)
        compose.setContent {
            FahrenheitTheme {
                MediaPlayerController(
                    player = player, playback = BookPlayback(player, twoParts), totalTime = twoParts.totalDuration,
                    skipBack = back, skipForward = forward
                )
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun `the skip buttons jump by the lengths set, and say so`() {
        showSkipping(back = 10, forward = 60)

        press(compose.activity.getString(R.string.skip_back_seconds, 10))
        compose.waitForIdle()
        assertEquals(590_000L, player.currentPosition)

        press(compose.activity.getString(R.string.skip_forward_seconds, 60))
        compose.waitForIdle()
        assertEquals(650_000L, player.currentPosition)
    }

    @Test
    fun `the skip icons show the number`() {
        showSkipping(back = 15, forward = 60)
        compose.onNodeWithText("15", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("60", useUnmergedTree = true).assertExists()
    }
}
