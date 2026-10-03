package com.paulohenriquesg.fahrenheit.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import com.paulohenriquesg.fahrenheit.detail.DESCRIPTION_TAG
import com.paulohenriquesg.fahrenheit.player.AboutFact
import com.paulohenriquesg.fahrenheit.player.SeriesBook
import com.paulohenriquesg.fahrenheit.player.SeriesBooks
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** One layout for the book screen and the player's About (#134; option 1 of docs/mocks/book-screen.html). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w960dp-h540dp")
class BookOverviewTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val chosen = mutableListOf<String>()
    private val three = SeriesBooks(
        listOf(SeriesBook("b1", "The Quiet Signal"), SeriesBook("b2", "The Long Drift"), SeriesBook("b3", "A Late Message")),
        currentId = "b2"
    )
    private val facts = listOf(AboutFact(AboutFact.Kind.ReadBy, "A Reader"), AboutFact(AboutFact.Kind.Length, "11 h 57 min"))
    private val long = (1..60).joinToString("") { "<p>Paragraph $it of a description far longer than its box.</p>" }

    private fun show(
        description: String? = "<p>A survey ship drifts into a quiet sector.</p>",
        series: SeriesBooks? = three,
        ask: Boolean = false,
        landOnDescription: Boolean = true,
        facts: List<AboutFact> = this.facts
    ) {
        compose.setContent {
            FahrenheitTheme {
                BookOverview(
                    itemId = "b2",
                    title = "The Long Drift",
                    byline = "An Author · read by A Reader",
                    description = description,
                    facts = facts,
                    series = series,
                    seriesName = "The Long Way",
                    onSeriesBook = { chosen += it.itemId },
                    askBeforeSwitching = ask,
                    landOnDescription = landOnDescription
                ) {
                    Button(onClick = {}) { Text("ACTION") }
                }
            }
        }
        compose.waitForIdle()
    }

    private fun press(text: String) {
        compose.onNodeWithText(text).performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
    }

    @Test fun `it shows the book, its actions, description, series and facts`() {
        show()
        // The title, and again under this book's cover in the series.
        compose.onAllNodesWithText("The Long Drift")[0].assertIsDisplayed()
        compose.onNodeWithText("An Author · read by A Reader").assertIsDisplayed()
        compose.onNodeWithText("ACTION").assertIsDisplayed()
        compose.onNodeWithText("A survey ship drifts into a quiet sector.").assertIsDisplayed()
        compose.onNodeWithText("THE LONG WAY · 3 BOOKS").assertIsDisplayed()
        compose.onNodeWithText("A Late Message").assertIsDisplayed()
    }

    @Test fun `the facts are a list of labels and values`() {
        show()
        compose.onNodeWithText("Read by").assertIsDisplayed()
        compose.onNodeWithText("A Reader").assertIsDisplayed()
        compose.onNodeWithText("Length").assertIsDisplayed()
        compose.onNodeWithText("11 h 57 min").assertIsDisplayed()
    }

    // Review Focus 1.
    @Test fun `in About, focus lands on the description`() {
        show(landOnDescription = true)
        compose.onNodeWithTag(DESCRIPTION_TAG).assertIsFocused()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun `right from the actions reaches the description, and down scrolls it`() {
        show(description = long, landOnDescription = false)
        compose.onNodeWithText("ACTION").performSemanticsAction(SemanticsActions.RequestFocus)
        compose.onNodeWithText("ACTION").performKeyInput { pressKey(Key.DirectionRight) }
        compose.waitForIdle()
        compose.onNodeWithTag(DESCRIPTION_TAG).assertIsFocused()
        compose.onNodeWithTag(DESCRIPTION_TAG).performKeyInput { pressKey(Key.DirectionDown) }
        compose.waitForIdle()
        compose.onNodeWithTag(DESCRIPTION_TAG).assertIsFocused()
    }

    @Test fun `this book is marked in the series`() {
        show()
        // The big cover says the same; the series' cover is the one that can be chosen.
        compose.onNode(hasContentDescription("The Long Drift") and hasClickAction()).assertIsSelected()
    }

    // Review Focus 2.
    @Test fun `a long series opens with this book in view`() {
        val many = SeriesBooks((1..38).map { SeriesBook("b$it", "Book number $it") }, currentId = "b30")
        show(series = many)
        compose.onNodeWithText("Book number 30").assertIsDisplayed()
    }

    @Test fun `on the book screen, choosing another book opens it`() {
        show(ask = false)
        compose.onNodeWithContentDescription("A Late Message").performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        assertEquals(listOf("b3"), chosen)
    }

    @Test fun `from the player, choosing another book asks first`() {
        show(ask = true)
        compose.onNodeWithContentDescription("A Late Message").performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        compose.onNodeWithText("Play A Late Message instead?").assertIsDisplayed()
        press("Play")
        assertEquals(listOf("b3"), chosen)
    }

    @Test fun `back from the question answers no`() {
        show(ask = true)
        compose.onNodeWithContentDescription("A Late Message").performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
        compose.onNodeWithText("Play A Late Message instead?").assertDoesNotExist()
        assertFalse(compose.activity.isFinishing)
        assertEquals(emptyList<String>(), chosen)
    }

    // Review Focus 4.
    @Test fun `no series, no description and no facts leave no empty boxes`() {
        show(description = null, series = null, facts = emptyList(), landOnDescription = false)
        compose.onNodeWithTag(DESCRIPTION_TAG).assertDoesNotExist()
        compose.onNodeWithText("BOOKS", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Read by").assertDoesNotExist()
    }
}
