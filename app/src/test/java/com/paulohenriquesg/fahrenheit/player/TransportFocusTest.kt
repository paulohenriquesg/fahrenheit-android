package com.paulohenriquesg.fahrenheit.player

import android.support.v4.media.session.MediaSessionCompat
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import org.junit.Before
import org.junit.Rule
import org.robolectric.shadows.ShadowMediaPlayer
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Seen on the stick: nothing on the player held focus, so no D-pad key reached
 * it - the transport, and "Go to podcast", could not be reached at all. Only
 * the remote's own play button worked, through the media session. Frame 4 of
 * the mocks: play is focused on arrival.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w960dp-h540dp")
class TransportFocusTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun aFileThatPrepares() {
        // Any source reads as a 30-minute file.
        ShadowMediaPlayer.setMediaInfoProvider { ShadowMediaPlayer.MediaInfo(1_800_000, 0) }
    }

    @Test
    fun `play holds focus on arrival`() {
        compose.setContent {
            FahrenheitTheme {
                MediaPlayerController(
                    url = "http://abs.invalid/never-prepares.mp3",
                    mediaSession = MediaSessionCompat(compose.activity, "test"),
                    isPlaying = false,
                    onPlayPause = {},
                    duration = 1800.0
                )
            }
        }
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Play").assertIsFocused()
    }
}
