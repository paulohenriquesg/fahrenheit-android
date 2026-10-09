package com.paulohenriquesg.fahrenheit.podcast

import androidx.activity.ComponentActivity
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
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
class FeedCheckButtonTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private var checks = 0

    private fun render(state: FeedCheckState) {
        compose.setContent {
            FahrenheitTheme { FeedCheckButton(state = state, onCheck = { checks++ }) }
        }
        compose.waitForIdle()
    }

    @OptIn(ExperimentalTestApi::class)
    private fun press() {
        compose.onNodeWithTag(FEED_CHECK_TAG).performSemanticsAction(SemanticsActions.RequestFocus)
        compose.onNodeWithTag(FEED_CHECK_TAG).performKeyInput { pressKey(Key.DirectionCenter) }
        compose.waitForIdle()
    }

    @Test
    fun `the button says what it will fetch before it fetches it`() {
        render(FeedCheckState.Idle)

        compose.onNodeWithText("Check for new episodes").assertIsDisplayed()
        compose.onNodeWithText("Up to 3 to the server").assertIsDisplayed()
    }

    @Test
    fun `pressing it asks for a check`() {
        render(FeedCheckState.Idle)

        press()

        assertEquals(1, checks)
    }

    @Test
    fun `a check in flight cannot be started twice`() {
        render(FeedCheckState.Checking)

        compose.onNodeWithText("Checking the feed…").assertIsDisplayed()
        press()

        assertEquals(0, checks)
    }

    @Test
    fun `what was found is counted`() {
        render(FeedCheckState.Found(3))

        compose.onNodeWithText("3 new, downloading").assertIsDisplayed()
    }

    @Test
    fun `one episode is counted as one`() {
        render(FeedCheckState.Found(1))

        compose.onNodeWithText("1 new, downloading").assertIsDisplayed()
    }

    @Test
    fun `nothing new is said as such`() {
        render(FeedCheckState.Found(0))

        compose.onNodeWithText("Nothing new").assertIsDisplayed()
    }

    @Test
    fun `a failure is said, and the check can be tried again`() {
        render(FeedCheckState.Failed)

        compose.onNodeWithText("Could not check").assertIsDisplayed()
        press()

        assertEquals(1, checks)
    }
}
