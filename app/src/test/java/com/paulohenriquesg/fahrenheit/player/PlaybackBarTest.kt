package com.paulohenriquesg.fahrenheit.player

import androidx.activity.ComponentActivity
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w960dp-h540dp")
class PlaybackBarTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun `a press seeks ten seconds, a held key further`() {
        assertEquals(10.0, SeekStep.seconds(0), 0.0)
        assertEquals(30.0, SeekStep.seconds(3), 0.0)
        assertEquals(60.0, SeekStep.seconds(10), 0.0)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun `left and right on a focused bar seek`() {
        val seeks = mutableListOf<Double>()
        compose.setContent { FahrenheitTheme { PlaybackBar(0.5f, Modifier.testTag("bar"), onSeekBy = { seeks += it }) } }
        compose.onNodeWithTag("bar").performSemanticsAction(SemanticsActions.RequestFocus)
        compose.onNodeWithTag("bar").assertIsFocused()

        compose.onNodeWithTag("bar").performKeyInput { pressKey(Key.DirectionRight) }
        compose.onNodeWithTag("bar").performKeyInput { pressKey(Key.DirectionLeft) }

        assertEquals(listOf(10.0, -10.0), seeks)
    }

    @Test fun `a bar that cannot seek takes no focus`() {
        compose.setContent { FahrenheitTheme { PlaybackBar(0.5f, Modifier.testTag("bar"), thick = false) } }
        compose.onNodeWithTag("bar").assert(androidx.compose.ui.test.SemanticsMatcher.keyNotDefined(SemanticsActions.RequestFocus))
    }
}
