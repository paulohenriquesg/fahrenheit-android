package com.paulohenriquesg.fahrenheit.settings

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import com.paulohenriquesg.fahrenheit.ui.theme.ThemePreference
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** "Play the next episode automatically", off by default (#108). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w960dp-h540dp")
class PlayNextEpisodeSettingTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun render(on: Boolean, onChange: (Boolean) -> Unit = {}) {
        compose.setContent {
            FahrenheitTheme {
                SettingsView(
                    theme = ThemePreference.System, onTheme = {}, rowLayout = true, onLayout = {},
                    version = "v0.0.10", update = UpdateCheck.Idle, onCheckUpdates = {},
                    username = "a listener", server = "http://books.example:13378", onSignOut = {},
                    playNextEpisode = on, onPlayNextEpisode = onChange
                )
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun `off, the row says off`() {
        render(on = false)
        compose.onNodeWithTag(PLAY_NEXT_EPISODE_TAG).performScrollTo().assertIsOff()
    }

    @Test
    fun `on, the row says on`() {
        render(on = true)
        compose.onNodeWithTag(PLAY_NEXT_EPISODE_TAG).performScrollTo().assertIsOn()
    }

    @Test
    fun `pressing it turns it on`() {
        val changed = mutableListOf<Boolean>()
        render(on = false) { changed += it }
        compose.onNodeWithTag(PLAY_NEXT_EPISODE_TAG).performScrollTo().performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        assertEquals(listOf(true), changed)
    }
}
