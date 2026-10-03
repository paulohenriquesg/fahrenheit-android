package com.paulohenriquesg.fahrenheit.player

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import com.paulohenriquesg.fahrenheit.api.Chapter
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The book's chapters, in a panel from the right; choosing one plays it (#107, #105). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w960dp-h540dp")
class ChapterListTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val chosen = mutableListOf<Double>()

    private val three = ChapterClock.spans(
        listOf(
            Chapter(start = 0.0, end = 600.0, title = "The Start"),
            Chapter(start = 600.0, end = 3900.0, title = ""),
            Chapter(start = 3900.0, end = 5400.0, title = "The End")
        ),
        total = 5400.0
    )

    private fun show(spans: List<ChapterSpan>, at: Double) {
        compose.setContent {
            FahrenheitTheme {
                val panels = rememberPlayerPanels()
                Box(Modifier.fillMaxSize()) {
                    ChaptersChip(panels)
                    PlayerPanelHost(panels) {
                        ChaptersPanel(spans, at, onChoose = { chosen += it }, onClose = panels::close)
                    }
                }
            }
        }
        compose.waitForIdle()
        compose.onNodeWithText("Chapters").performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
    }

    @Test fun `each chapter shows its title and where it starts`() {
        show(three, at = 0.0)
        compose.onNodeWithText("The End").assertIsDisplayed()
        compose.onNodeWithText("1 h 5 min").assertIsDisplayed()
    }

    @Test fun `a chapter without a title is numbered`() {
        show(three, at = 0.0)
        compose.onNodeWithText("Chapter 2").assertIsDisplayed()
    }

    @Test fun `it opens on the chapter playing`() {
        show(three, at = 4000.0)
        compose.onNodeWithText("The End").assertIsFocused()
    }

    @Test fun `choosing a chapter plays from its start and closes the panel`() {
        show(three, at = 0.0)
        compose.onNodeWithText("The End").performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        assertEquals(listOf(3900.0), chosen)
        compose.onNodeWithText("The End").assertDoesNotExist()
        compose.onNodeWithText("Chapters").assertIsFocused()
    }

    // Review Focus 5.
    @Test fun `a long list opens scrolled to the chapter playing`() {
        val sixty = ChapterClock.spans((0 until 60).map { Chapter(start = it * 600.0, end = (it + 1) * 600.0, title = "Part ${it + 1}") }, total = 36_000.0)
        show(sixty, at = 49 * 600.0 + 10)
        compose.onNodeWithText("Part 50").assertIsDisplayed()
        compose.onNodeWithText("Part 50").assertIsFocused()
    }
}
