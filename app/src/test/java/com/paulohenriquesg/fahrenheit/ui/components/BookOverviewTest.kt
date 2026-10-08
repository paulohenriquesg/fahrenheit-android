package com.paulohenriquesg.fahrenheit.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.onRoot
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.unit.Dp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.paulohenriquesg.fahrenheit.detail.DESCRIPTION_TAG
import com.paulohenriquesg.fahrenheit.player.AboutFact
import com.paulohenriquesg.fahrenheit.player.SeriesBook
import com.paulohenriquesg.fahrenheit.player.SeriesBooks
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import androidx.compose.ui.unit.dp
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
        facts: List<AboutFact> = this.facts,
        title: String = "The Long Drift",
        actions: Int = 1
    ) {
        compose.setContent {
            FahrenheitTheme {
                primary = MaterialTheme.colorScheme.primary
                BookOverview(
                    itemId = "b2",
                    title = title,
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
                    repeat(actions - 1) { Button(onClick = {}) { Text("ACTION ${it + 2}") } }
                }
            }
        }
        compose.waitForIdle()
    }

    private var primary = Color.Unspecified

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

    // Review (#134): Down from the description went to a neighbouring cover.
    @OptIn(ExperimentalTestApi::class)
    @Test fun `down from the description lands on this book in the series`() {
        // Ten books, this the second: the cover under the description's middle is another one.
        val ten = SeriesBooks(
            listOf(SeriesBook("b1", "The Quiet Signal"), SeriesBook("b2", "The Long Drift")) +
                (3..10).map { SeriesBook("b$it", "Book number $it") },
            currentId = "b2"
        )
        show(series = ten, landOnDescription = true)
        compose.onNodeWithTag(DESCRIPTION_TAG).performKeyInput { pressKey(Key.DirectionDown) }
        compose.waitForIdle()
        compose.onNode(hasContentDescription("The Long Drift") and hasClickAction()).assertIsFocused()
    }

    // #170: the focus border sat on the text. The text keeps clear of the 3dp
    // border by more than the border itself.
    @Test fun `the description has room inside its focus border`() {
        show()
        val node = compose.onNodeWithTag(DESCRIPTION_TAG).fetchSemanticsNode()
        val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
        node.config[androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult].action!!.invoke(layouts)
        // Its padding is all round; above and below, the text's own height is exact.
        val inset = (node.size.height - layouts.single().size.height) / 2f
        val wanted = with(compose.density) { 11.dp.toPx() }
        assertTrue("inset ${inset}px, wanted ${wanted}px", inset >= wanted)
    }

    // #178: the box's text stopped mid-line and looked clipped, not scrollable.
    @Test fun `a long description fades out at the bottom`() {
        show(description = long)
        compose.onNodeWithTag(DESCRIPTION_BOX_TAG).assert(SemanticsMatcher.expectValue(DescriptionFadesOut, true))
    }

    @Test fun `a description that fits does not fade`() {
        show()
        compose.onNodeWithTag(DESCRIPTION_BOX_TAG).assert(SemanticsMatcher.expectValue(DescriptionFadesOut, false))
    }

    @Test fun `scrolled to its end, the description no longer fades`() {
        show(description = long)
        compose.onNodeWithTag(DESCRIPTION_BOX_TAG).performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, 1_000_000f) }
        compose.waitForIdle()
        compose.onNodeWithTag(DESCRIPTION_BOX_TAG).assert(SemanticsMatcher.expectValue(DescriptionFadesOut, false))
    }

    private fun bounds(tag: String) = compose.onNodeWithTag(tag).getUnclippedBoundsInRoot()
    private val screenBottom: Dp get() = compose.onRoot().getUnclippedBoundsInRoot().bottom

    /** The series and facts at the bottom, the description box reaching down to just above them. */
    private fun assertDetailsAtTheBottom(descriptionAbove: Boolean) {
        val lower = bounds(BOOK_LOWER_TAG)
        assertEquals(screenBottom.value, lower.bottom.value, 1f)
        if (descriptionAbove) {
            val box = bounds(DESCRIPTION_BOX_TAG)
            assertTrue("box ends at ${box.bottom}, details start at ${lower.top}", box.bottom <= lower.top)
            assertTrue("box ends at ${box.bottom}, details start at ${lower.top}", lower.top - box.bottom <= 24.5.dp)
        }
    }

    // #194: the bottom third of the screen sat empty under a cut-off description.
    @Test fun `the series and facts sit at the bottom, the description fills the space above`() {
        show()
        assertDetailsAtTheBottom(descriptionAbove = true)
    }

    @Test fun `with no series, the facts still sit at the bottom under the description`() {
        show(series = null)
        assertDetailsAtTheBottom(descriptionAbove = true)
    }

    @Test fun `with no description, the details still sit at the bottom`() {
        show(description = null, landOnDescription = false)
        assertDetailsAtTheBottom(descriptionAbove = false)
    }

    @Test fun `series cards are labelled by their place in the series`() {
        show(
            series = SeriesBooks(
                listOf(SeriesBook("b1", "The Quiet Signal", "1"), SeriesBook("b2", "The Long Drift", "2"), SeriesBook("b3", "A Late Message", "2.5")),
                currentId = "b2"
            )
        )
        compose.onNodeWithText("Book 1").assertIsDisplayed()
        compose.onNodeWithText("Book 2").assertIsDisplayed()
        compose.onNodeWithText("Book 2.5").assertIsDisplayed()
        compose.onNodeWithText("A Late Message").assertDoesNotExist()
    }

    // #194: the row showed four of seven, with nothing to say there were more.
    @OptIn(ExperimentalTestApi::class)
    @Test fun `right on the series row reaches the last book`() {
        val seven = SeriesBooks((1..7).map { SeriesBook("b$it", "Book number $it", "$it") }, currentId = "b1")
        show(series = seven, landOnDescription = false)
        compose.onNode(hasContentDescription("Book number 1") and hasClickAction()).performSemanticsAction(SemanticsActions.RequestFocus)
        repeat(6) {
            compose.onNode(isFocused()).performKeyInput { pressKey(Key.DirectionRight) }
            compose.waitForIdle()
        }
        compose.onNode(hasContentDescription("Book number 7") and hasClickAction()).assertIsFocused().assertIsDisplayed()
    }

    // #194: the focus border ran on under the fade, past the box's bottom.
    @Test fun `the description's focus border is drawn whole`() {
        show(description = long, landOnDescription = true)
        val box = compose.onNodeWithTag(DESCRIPTION_BOX_TAG).fetchSemanticsNode()
        val at = box.positionInWindow
        val pixels = window()
        val inBorder = with(compose.density) { 1.5.dp.roundToPx() }
        val x = at.x.toInt() + box.size.width / 2
        listOf(at.y.toInt() + box.size.height - 1 - inBorder, at.y.toInt() + inBorder).forEach { y ->
            val seen = pixels[x, y]
            assertEquals("red at $x,$y", primary.red, seen.red, 0.05f)
            assertEquals("green at $x,$y", primary.green, seen.green, 0.05f)
            assertEquals("blue at $x,$y", primary.blue, seen.blue, 0.05f)
        }
    }

    // captureToImage waits for a frame callback Robolectric never sends, so the
    // window is drawn into a bitmap directly (as NowPlayingStateTest does).
    private fun window(): PixelMap {
        lateinit var map: PixelMap
        compose.runOnUiThread {
            val view = compose.activity.window.decorView
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            map = bitmap.asImageBitmap().toPixelMap()
        }
        return map
    }

    private fun descriptionText(): String = compose.onNodeWithTag(DESCRIPTION_TAG).fetchSemanticsNode()
        .config[androidx.compose.ui.semantics.SemanticsProperties.Text].joinToString("") { it.text }

    private val threeLines = "A Remarkably Long Title for One Book"
    private val tooLong = (1..12).joinToString(" ") { "Chapter-length words $it" }

    // #194: the title was cut at two lines, and the full title was nowhere on the page.
    @Test fun `a long title takes three lines`() {
        show(title = threeLines)
        val node = compose.onNodeWithTag(TITLE_TAG).fetchSemanticsNode()
        val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
        node.config[SemanticsActions.GetTextLayoutResult].action!!.invoke(layouts)
        assertEquals(3, layouts.single().lineCount)
        assertFalse(layouts.single().hasVisualOverflow)
    }

    @Test fun `a title cut even at three lines is shown whole above the description`() {
        show(title = tooLong)
        assertTrue(descriptionText(), descriptionText().startsWith("$tooLong\n\nA survey ship"))
    }

    @Test fun `with no description, a cut title is shown whole where it would be`() {
        show(title = tooLong, description = null, landOnDescription = false)
        // The cut one under the cover, and the whole one on the right.
        compose.onAllNodesWithText(tooLong).assertCountEquals(2)
        compose.onNodeWithTag(FULL_TITLE_TAG).assertIsDisplayed()
    }

    @Test fun `with no description, a title that fits is not repeated`() {
        show(title = threeLines, description = null, landOnDescription = false)
        compose.onNodeWithTag(FULL_TITLE_TAG).assertDoesNotExist()
    }

    @Test fun `a title that fits is not repeated above the description`() {
        show(title = threeLines)
        assertEquals("A survey ship drifts into a quiet sector.", descriptionText())
    }

    @Test fun `three actions still fit under a three-line title`() {
        show(title = threeLines, actions = 3)
        assertTrue(compose.onNodeWithText("ACTION 3").getUnclippedBoundsInRoot().bottom <= screenBottom)
    }

    // #194: a focused card's cut label scrolls, a few times and then rests,
    // so the screen (and this test) still goes idle.
    @Test fun `only the focused series card's label scrolls`() {
        val long = SeriesBooks(
            listOf(SeriesBook("b1", "A Title Far Too Long for Its Card"), SeriesBook("b2", "The Long Drift"), SeriesBook("b3", "Another Title Too Long to Fit")),
            currentId = "b2"
        )
        show(series = long, landOnDescription = false)
        fun scrolls(label: String) = compose.onNodeWithText(label).fetchSemanticsNode().config.getOrElse(SeriesLabelScrolls) { false }

        assertFalse(scrolls("A Title Far Too Long for Its Card"))
        compose.onNode(hasContentDescription("A Title Far Too Long for Its Card") and hasClickAction()).performSemanticsAction(SemanticsActions.RequestFocus)
        compose.waitForIdle()

        assertTrue(scrolls("A Title Far Too Long for Its Card"))
        assertFalse(scrolls("Another Title Too Long to Fit"))
    }
}
