package com.paulohenriquesg.fahrenheit.settings

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
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

/** Skip back and Skip forward: 10, 15, 30 or 60 s, in the Playback group (#107). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w960dp-h540dp")
class SkipLengthSettingTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val back = mutableListOf<Int>()
    private val forward = mutableListOf<Int>()

    private fun render(skipBack: Int = 30, skipForward: Int = 30) {
        compose.setContent {
            FahrenheitTheme {
                SettingsView(
                    theme = ThemePreference.System, onTheme = {}, rowLayout = true, onLayout = {},
                    version = "v0.0.10", update = UpdateCheck.Idle, onCheckUpdates = {}, onInstall = {},
                    username = "a listener", server = "http://books.example:13378", onSignOut = {},
                    deviceName = "Living room", onDeviceName = {},
                    skipBack = skipBack, onSkipBack = { back += it },
                    skipForward = skipForward, onSkipForward = { forward += it }
                )
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun `each row offers the four lengths, the one set ticked`() {
        render(skipBack = 15, skipForward = 60)
        listOf(10, 15, 30, 60).forEach {
            compose.onNodeWithTag("skip_back_$it").performScrollTo()
            compose.onNodeWithTag("skip_forward_$it").performScrollTo()
        }
        compose.onNodeWithText("15 s ✓").assertExists()
        compose.onNodeWithText("60 s ✓").assertExists()
    }

    @Test
    fun `choosing a length reports it`() {
        render()
        compose.onNodeWithTag("skip_back_10").performScrollTo().performSemanticsAction(SemanticsActions.OnClick)
        compose.onNodeWithTag("skip_forward_60").performScrollTo().performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        assertEquals(listOf(10), back)
        assertEquals(listOf(60), forward)
    }
}
