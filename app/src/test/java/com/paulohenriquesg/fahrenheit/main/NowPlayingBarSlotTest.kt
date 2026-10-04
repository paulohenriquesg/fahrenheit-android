package com.paulohenriquesg.fahrenheit.main

import androidx.activity.ComponentActivity
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.test.utils.TestExoPlayerBuilder
import androidx.media3.test.utils.robolectric.TestPlayerRunHelper.run
import com.paulohenriquesg.fahrenheit.player.NowPlaying
import com.paulohenriquesg.fahrenheit.player.PlaybackQueue
import com.paulohenriquesg.fahrenheit.player.TimelineTrack
import com.paulohenriquesg.fahrenheit.player.TrackTimeline
import com.paulohenriquesg.fahrenheit.player.hourLongFiles
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The bar reads what the screen's player has queued; its Stop ends it (#159). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w960dp-h540dp")
class NowPlayingBarSlotTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private lateinit var player: ExoPlayer

    @After
    fun tearDown() = player.release()

    private fun show() {
        compose.setContent {
            FahrenheitTheme {
                Column {
                    NowPlayingBarSlot(player, chaptersOf = { null }, onOpen = {})
                    BasicText("the screen", Modifier.testTag("content").focusable())
                }
            }
        }
        run(player).untilPendingCommandsAreFullyHandled()
        compose.waitForIdle()
    }

    private fun queue() {
        val book = NowPlaying("b1", "A Book", TrackTimeline(listOf(TimelineTrack(1, 0.0, 3600.0, "/b1"))), null, null, null, false, null)
        val queue = PlaybackQueue.of(book, 600.0) { "https://abs.test$it" }!!
        player.setMediaItems(queue.items, queue.index, queue.positionMs)
        player.prepare()
        player.play()
    }

    @Test
    fun `nothing queued, no bar`() {
        player = TestExoPlayerBuilder(compose.activity).build()
        show()
        compose.onNodeWithTag(NOW_PLAYING_BAR_TAG).assertDoesNotExist()
    }

    @Test
    fun `no controller, no bar`() {
        player = TestExoPlayerBuilder(compose.activity).build()
        compose.setContent { FahrenheitTheme { NowPlayingBarSlot(null, chaptersOf = { null }, onOpen = {}) } }
        compose.waitForIdle()
        compose.onNodeWithTag(NOW_PLAYING_BAR_TAG).assertDoesNotExist()
    }

    @Test
    fun `Stop empties the queue, the bar goes, and focus moves into the screen`() {
        player = TestExoPlayerBuilder(compose.activity).setMediaSourceFactory(hourLongFiles()).build()
        queue()
        show()
        compose.onNodeWithTag(NOW_PLAYING_BAR_TAG).assertExists()
        compose.onNodeWithTag(NOW_PLAYING_BAR_STOP_TAG).performSemanticsAction(SemanticsActions.RequestFocus)
        compose.waitForIdle()

        compose.onNodeWithTag(NOW_PLAYING_BAR_STOP_TAG).performSemanticsAction(SemanticsActions.OnClick)
        run(player).untilPendingCommandsAreFullyHandled()
        compose.waitForIdle()

        assertEquals(0, player.mediaItemCount)
        compose.onNodeWithTag(NOW_PLAYING_BAR_TAG).assertDoesNotExist()
        compose.onNodeWithTag("content").assertIsFocused()
    }
}
