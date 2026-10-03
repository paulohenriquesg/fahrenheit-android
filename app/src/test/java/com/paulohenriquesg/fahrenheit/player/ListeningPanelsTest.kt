package com.paulohenriquesg.fahrenheit.player

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The Speed and Sleep chips, and the panel each opens from the right
 * (#107; frame "A, with a panel open: Speed").
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w960dp-h540dp")
class ListeningPanelsTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val note = "Remembered for this book. The time left in the book counts at this speed."
    private val chosen = mutableListOf<SleepChoice>()

    /** The chips and their panels, as the player lays them out. */
    private fun show(speedAt: Float = 1.25f, sleep: SleepState? = null, chapters: Boolean = true) {
        compose.setContent {
            FahrenheitTheme {
                var speed by remember { mutableFloatStateOf(speedAt) }
                val panels = rememberPlayerPanels()
                Box(Modifier.fillMaxSize()) {
                    Row {
                        SpeedChip(speed, panels)
                        SleepChip(sleep, panels)
                    }
                    PlayerPanelHost(panels) { panel ->
                        when (panel) {
                            PlayerPanel.Speed -> SpeedPanel(speed, onChoose = { speed = it }, onClose = panels::close)
                            PlayerPanel.Sleep -> SleepPanel(sleep, chapters, onChoose = { chosen += it }, onClose = panels::close)
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    private fun press(text: String) {
        compose.onNodeWithText(text).performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
    }

    @Test fun `the speed chip names the speed`() {
        show(speedAt = 1.25f)
        compose.onNodeWithText("Speed 1.25×").assertIsDisplayed()
    }

    @Test fun `the sleep chip shows the minutes left while a timer runs`() {
        show(sleep = SleepState(SleepChoice.Minutes(15), minutesLeft = 12))
        compose.onNodeWithText("Sleep 12 min").assertIsDisplayed()
    }

    @Test fun `the sleep chip is plain while no timer runs`() {
        show()
        compose.onNodeWithText("Sleep").assertIsDisplayed()
    }

    @Test fun `speed opens on the chosen speed`() {
        show(speedAt = 1.25f)
        press("Speed 1.25×")
        compose.onNodeWithText(note).assertIsDisplayed()
        compose.onNodeWithText("1.25×").assertIsFocused()
    }

    @Test fun `choosing a speed closes the panel and focus returns to the chip`() {
        show(speedAt = 1.25f)
        press("Speed 1.25×")
        press("1.5×")
        compose.onNodeWithText(note).assertDoesNotExist()
        compose.onNodeWithText("Speed 1.5×").assertIsFocused()
    }

    // Review Focus 3.
    @Test fun `back closes the panel, not the screen`() {
        show()
        press("Speed 1.25×")
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
        compose.onNodeWithText(note).assertDoesNotExist()
        assertFalse(compose.activity.isFinishing)
        compose.onNodeWithText("Speed 1.25×").assertIsFocused()
    }

    // Review Focus 4.
    @OptIn(ExperimentalTestApi::class)
    @Test fun `focus stays in the panel`() {
        show(speedAt = 1f)
        press("Speed 1×")
        compose.onNodeWithText("1×").performKeyInput { pressKey(Key.DirectionLeft) }
        compose.waitForIdle()
        compose.onNodeWithText("1×").assertIsFocused()
    }

    @Test fun `sleep offers the end of the chapter when there are chapters`() {
        show(chapters = true)
        press("Sleep")
        press("End of chapter")
        assertEquals(listOf<SleepChoice>(SleepChoice.EndOfChapter), chosen)
    }

    @Test fun `without chapters there is no end of chapter`() {
        show(chapters = false)
        press("Sleep")
        compose.onNodeWithText("End of chapter").assertDoesNotExist()
        compose.onNodeWithText("30 min").assertIsDisplayed()
    }

    @Test fun `sleep opens on the running choice, and off when none runs`() {
        show(sleep = SleepState(SleepChoice.Minutes(30), minutesLeft = 21))
        press("Sleep 21 min")
        compose.onNodeWithText("30 min").assertIsFocused()
    }

    @Test fun `with no timer, sleep opens on off`() {
        show()
        press("Sleep")
        compose.onNodeWithText("Off").assertIsFocused()
    }

    // Review: Up and Down at the panel's ends are the edges a listener hits.
    @OptIn(ExperimentalTestApi::class)
    @Test fun `focus stays in the panel at its top and bottom`() {
        show(speedAt = 0.75f)
        press("Speed 0.75×")
        compose.onNodeWithText("0.75×").performKeyInput { pressKey(Key.DirectionUp) }
        compose.waitForIdle()
        compose.onNodeWithText("0.75×").assertIsFocused()
    }

    @Test fun `a show's speed is remembered for the show`() {
        compose.setContent {
            FahrenheitTheme { SpeedPanel(1f, forShow = true, onChoose = {}, onClose = {}) }
        }
        compose.onNodeWithText("Remembered for this show. The time left counts at this speed.").assertIsDisplayed()
    }

    // Review: a panel left open when the player went away came back over it.
    @Test fun `panels start closed for a new connection`() {
        var connection by mutableStateOf(1)
        lateinit var panels: PlayerPanels
        compose.setContent { panels = rememberPlayerPanels(connection) }
        compose.runOnUiThread { panels.open(PlayerPanel.Speed) }
        compose.waitForIdle()

        connection = 2
        compose.waitForIdle()

        assertEquals(null, panels.open)
    }
}
