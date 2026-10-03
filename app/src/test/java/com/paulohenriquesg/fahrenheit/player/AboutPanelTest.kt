package com.paulohenriquesg.fahrenheit.player

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import com.paulohenriquesg.fahrenheit.detail.DESCRIPTION_TAG
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** About, from the player (#107; frame "A, with About open"). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w960dp-h540dp")
class AboutPanelTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val switched = mutableListOf<String>()
    private val marked = mutableListOf<Boolean>()

    private val series = SeriesBooks(
        listOf(SeriesBook("b1", "The First"), SeriesBook("b2", "The Second"), SeriesBook("b3", "The Third")),
        currentId = "b2"
    )
    private val facts = listOf(AboutFact(AboutFact.Kind.ReadBy, "A Reader"), AboutFact(AboutFact.Kind.Published, "2015"))
    private val long = (1..60).joinToString("") { "<p>Paragraph $it of a description far longer than the panel.</p>" }

    private fun show(
        description: String? = "<p>A short description.</p>",
        series: SeriesBooks? = this.series,
        finishedAt: Boolean? = false
    ) {
        compose.setContent {
            FahrenheitTheme {
                val panels = rememberPlayerPanels()
                var finished by remember { mutableStateOf(finishedAt) }
                Box(Modifier.fillMaxSize()) {
                    AboutChip(panels)
                    PlayerPanelHost(panels) {
                        AboutPanel(
                            description = description,
                            facts = facts,
                            series = series,
                            finished = finished,
                            onPlayInstead = { switched += it.itemId },
                            onMarkFinished = { marked += it; finished = it },
                            onClose = panels::close
                        )
                    }
                }
            }
        }
        compose.waitForIdle()
        press("About")
    }

    private fun press(text: String) {
        compose.onNodeWithText(text).performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
    }

    // Review Focus 4.
    @Test fun `focus lands on the description`() {
        show()
        compose.onNodeWithTag(DESCRIPTION_TAG).assertIsFocused()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun `down scrolls a long description rather than leaving it`() {
        show(description = long)
        compose.onNodeWithTag(DESCRIPTION_TAG).performKeyInput { pressKey(Key.DirectionDown) }
        compose.waitForIdle()
        compose.onNodeWithTag(DESCRIPTION_TAG).assertIsFocused()
    }

    @Test fun `the facts read one line each`() {
        show()
        compose.onNodeWithText("Read by A Reader").assertIsDisplayed()
        compose.onNodeWithText("Published 2015").assertIsDisplayed()
    }

    @Test fun `the series shows each book, this one marked`() {
        show()
        compose.onNodeWithContentDescription("The First").assertIsDisplayed()
        compose.onNodeWithContentDescription("The Second").assertIsSelected()
    }

    @Test fun `choosing another book asks first, and Play switches`() {
        show()
        compose.onNodeWithContentDescription("The Third").performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        compose.onNodeWithText("Play The Third instead?").assertIsDisplayed()
        press("Play")
        assertEquals(listOf("b3"), switched)
    }

    @Test fun `this book asks nothing`() {
        show()
        compose.onNodeWithContentDescription("The Second").performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        compose.onNodeWithText("Play The Second instead?").assertDoesNotExist()
    }

    @Test fun `cancel goes back to the book chosen`() {
        show()
        compose.onNodeWithContentDescription("The Third").performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        press("Cancel")
        compose.onNodeWithText("Play The Third instead?").assertDoesNotExist()
        compose.onNodeWithContentDescription("The Third").assertIsFocused()
        assertEquals(emptyList<String>(), switched)
    }

    // Review Focus 4.
    @Test fun `back from the question returns to the series, not out of the panel`() {
        show()
        compose.onNodeWithContentDescription("The Third").performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
        compose.onNodeWithText("Play The Third instead?").assertDoesNotExist()
        compose.onNodeWithText("Read by A Reader").assertIsDisplayed()
        assertFalse(compose.activity.isFinishing)
    }

    @Test fun `mark finished, then unfinished`() {
        show()
        press("Mark finished")
        press("Mark unfinished")
        assertEquals(listOf(true, false), marked)
    }

    // Review Focus 3.
    @Test fun `a book in no series has no series row`() {
        show(series = null)
        compose.onNodeWithText("The series").assertDoesNotExist()
    }

    @Test fun `an episode has no Mark finished`() {
        show(series = null, finishedAt = null)
        compose.onNodeWithText("Mark finished").assertDoesNotExist()
        compose.onNodeWithText("Mark unfinished").assertDoesNotExist()
    }
}
