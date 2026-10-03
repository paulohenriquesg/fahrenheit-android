package com.paulohenriquesg.fahrenheit.detail

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import com.paulohenriquesg.fahrenheit.api.Chapter
import com.paulohenriquesg.fahrenheit.player.ChapterClock
import com.paulohenriquesg.fahrenheit.player.ChapterSpan
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** A book's details screen: Mark finished and Chapters beside Resume (#105; frame 3). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w960dp-h540dp")
class DetailActionsTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val chosen = mutableListOf<Double>()
    private val marked = mutableListOf<Boolean>()

    private val three = ChapterClock.spans(
        listOf(
            Chapter(start = 0.0, end = 600.0, title = "The Start"),
            Chapter(start = 600.0, end = 1800.0, title = "The Middle"),
            Chapter(start = 1800.0, end = 2400.0, title = "The End")
        ),
        total = 2400.0
    )

    private fun show(chapters: List<ChapterSpan> = three, at: Double = 700.0, finishedAt: Boolean = false) {
        compose.setContent {
            FahrenheitTheme {
                var finished by remember { mutableStateOf(finishedAt) }
                BookDetailView(
                    itemId = "b1",
                    content = DetailHeaderContent("A Book", null, emptyList(), "Resume at 11 min", null),
                    onPrimary = {},
                    chapters = chapters,
                    at = at,
                    onChapter = { chosen += it },
                    finished = finished,
                    onMarkFinished = { marked += it; finished = it }
                )
            }
        }
        compose.waitForIdle()
    }

    private fun press(text: String) {
        compose.onNodeWithText(text).performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
    }

    @Test fun `resume still holds focus on arrival`() {
        show()
        compose.onNodeWithTag(PRIMARY_ACTION_TAG).assertIsFocused()
    }

    @Test fun `mark finished, then unfinished`() {
        show()
        press("Mark finished")
        press("Mark unfinished")
        assertEquals(listOf(true, false), marked)
    }

    @Test fun `chapters opens on the chapter the book was left in`() {
        show(at = 700.0)
        press("Chapters")
        compose.onNodeWithText("The Middle").assertIsFocused()
    }

    @Test fun `choosing a chapter plays it`() {
        show()
        press("Chapters")
        press("The End")
        assertEquals(listOf(1800.0), chosen)
    }

    @Test fun `back closes the chapters, not the screen`() {
        show()
        press("Chapters")
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
        compose.onNodeWithText("The Middle").assertDoesNotExist()
        assertFalse(compose.activity.isFinishing)
        compose.onNodeWithText("Chapters").assertIsFocused()
    }

    // Review Focus 3.
    @Test fun `a book without chapters has no chapters button`() {
        show(chapters = emptyList())
        compose.onNodeWithText("Chapters").assertDoesNotExist()
        compose.onNodeWithText("Mark finished").assertIsDisplayed()
    }
}
