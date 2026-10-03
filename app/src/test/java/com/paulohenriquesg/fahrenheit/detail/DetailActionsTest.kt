package com.paulohenriquesg.fahrenheit.detail

import androidx.compose.ui.test.onNodeWithContentDescription
import com.paulohenriquesg.fahrenheit.player.SeriesBooks
import com.paulohenriquesg.fahrenheit.player.SeriesBook
import com.paulohenriquesg.fahrenheit.player.AboutFact
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import com.paulohenriquesg.fahrenheit.api.Chapter
import com.paulohenriquesg.fahrenheit.player.ChapterClock
import com.paulohenriquesg.fahrenheit.player.ChapterSpan
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import com.paulohenriquesg.fahrenheit.player.SIDE_PANEL_SCRIM_TAG
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

    private fun show(chapters: List<ChapterSpan> = three, at: Double = 700.0, finishedAt: Boolean = false, marking: Boolean = false) {
        compose.setContent {
            FahrenheitTheme {
                var finished by remember { mutableStateOf(finishedAt) }
                // As the screen does once the book is read again: a finished book just plays.
                val primary = if (finished) "Play" else "Resume at 11 min"
                BookDetailView(
                    itemId = "b1",
                    content = DetailHeaderContent("A Book", null, emptyList(), primary, null),
                    marking = marking,
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

    // Review: Resume's label changing took focus off the button just pressed.
    @Test fun `focus stays on Mark finished when Resume's label changes`() {
        show()
        compose.onNodeWithText("Mark finished").performSemanticsAction(SemanticsActions.RequestFocus)
        press("Mark finished")
        compose.onNodeWithText("Mark unfinished").assertIsFocused()
    }

    @Test fun `while a mark is being made, the button does nothing`() {
        show(marking = true)
        press("Mark finished")
        assertEquals(emptyList<Boolean>(), marked)
    }

    // Device check: the scrim stopped short of the screen's edges. Laid out
    // as the details screen lays out a book, through DetailBody.
    @Test fun `the chapters panel covers the whole screen, the book keeps its margins`() {
        compose.setContent {
            FahrenheitTheme {
                DetailBody(isBook = true) { margin ->
                    BookDetailView(
                        itemId = "b1",
                        content = DetailHeaderContent("A Book", null, emptyList(), "Play", null),
                        onPrimary = {},
                        chapters = three,
                        padding = margin
                    )
                }
            }
        }
        compose.waitForIdle()
        press("Chapters")

        val screen = compose.onRoot().getUnclippedBoundsInRoot()
        val scrim = compose.onNodeWithTag(SIDE_PANEL_SCRIM_TAG).getUnclippedBoundsInRoot()
        assertEquals(screen, scrim)
        // The book itself still sits inside the margin: its cover starts 24 dp in.
        compose.onNodeWithContentDescription("A Book").assertLeftPositionInRootIsEqualTo(24.dp)
    }

    private val series = SeriesBooks(
        listOf(SeriesBook("b0", "The Quiet Signal"), SeriesBook("b1", "A Book"), SeriesBook("b2", "A Late Message")),
        currentId = "b1"
    )

    private fun showOverview(onSeries: (String) -> Unit = {}) {
        compose.setContent {
            FahrenheitTheme {
                BookDetailView(
                    itemId = "b1",
                    content = DetailHeaderContent("A Book", "An Author", emptyList(), "Resume at 11 min", "<p>A short blurb.</p>"),
                    onPrimary = {},
                    chapters = three,
                    finished = false,
                    facts = listOf(AboutFact(AboutFact.Kind.ReadBy, "A Reader"), AboutFact(AboutFact.Kind.Progress, "34% in")),
                    series = series,
                    seriesName = "The Long Way",
                    onSeriesBook = { onSeries(it.itemId) }
                )
            }
        }
        compose.waitForIdle()
    }

    // #134: the book screen is the one book layout.
    @Test fun `the book screen shows the series and the facts, progress among them`() {
        showOverview()
        compose.onNodeWithText("THE LONG WAY · 3 BOOKS").assertIsDisplayed()
        compose.onNodeWithText("Progress").assertIsDisplayed()
        compose.onNodeWithText("34% in").assertIsDisplayed()
        compose.onNodeWithTag(PRIMARY_ACTION_TAG).assertIsFocused()
    }

    @Test fun `choosing another book in the series opens it, without asking`() {
        val opened = mutableListOf<String>()
        showOverview { opened += it }
        compose.onNodeWithContentDescription("A Late Message").performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        assertEquals(listOf("b2"), opened)
    }
}
