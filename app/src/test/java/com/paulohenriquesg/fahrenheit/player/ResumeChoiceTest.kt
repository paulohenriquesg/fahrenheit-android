package com.paulohenriquesg.fahrenheit.player

import androidx.activity.ComponentActivity
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.dp
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The question (#90), as the card of #158 (docs/mocks/resume-question.html):
 * who moved it and when, both places on a small timeline, chapters on a
 * book's buttons, and focus on the newer position.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w960dp-h540dp")
class ResumeChoiceTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val now = 1_700_000_000_000L
    private val minute = 60_000L
    private var chosen: String? = null

    private val chapters = listOf(
        ChapterSpan("Chapter 8", 3600.0, 4200.0),
        ChapterSpan("Chapter 9", 4200.0, 4500.0),
        ChapterSpan("Chapter 12", 4500.0, 5400.0),
        ChapterSpan("Chapter 13", 5400.0, 7200.0)
    )
    private val book = ResumeItem(itemId = "b1", title = "A Made-Up Book", length = 7200.0, chapters = chapters, episode = false)
    private val episode = ResumeItem(itemId = "p1", title = "Episode 1", length = 3200.0, chapters = emptyList(), episode = true)

    private fun offer(here: Double = 3900.0, there: Double = 4800.0, ago: Long = 10 * minute, device: String? = "iPhone") =
        ResumeOffer(here = here, there = there, listenedAt = now - ago, device = device)

    private fun words(offer: ResumeOffer = offer(), item: ResumeItem = book) =
        resumeWording(compose.activity.resources, offer, item, now)

    private fun show(offer: ResumeOffer = offer(), item: ResumeItem = book) {
        compose.setContent {
            FahrenheitTheme {
                // Something behind the question, as the transport is.
                androidx.compose.foundation.layout.Box {
                    androidx.tv.material3.Button(onClick = {}, modifier = androidx.compose.ui.Modifier.testTag("behind")) {
                        androidx.tv.material3.Text("behind")
                    }
                    ResumeChoice(offer, item, now, onContinue = { chosen = "there" }, onStay = { chosen = "here" })
                }
            }
        }
        compose.waitForIdle()
    }

    private fun text(tag: String) = compose.onNodeWithTag(tag).fetchSemanticsNode()
        .config[SemanticsProperties.Text].joinToString(" ") { it.text }

    @Test
    fun `a book heard further on a phone names the phone, the chapters and both times`() {
        assertEquals(
            ResumeWording(
                headline = "You listened further on iPhone",
                heard = "10 minutes ago · 1 h 20 min there, 1 h 05 min here",
                hereMark = "here · Chapter 8",
                thereMark = "iPhone · Chapter 12",
                continueLabel = "Continue from Chapter 12",
                continueDetail = "1 h 20 min · where iPhone left off",
                stayLabel = "Stay at Chapter 8",
                stayDetail = "1 h 05 min · where this TV left off"
            ),
            words()
        )
    }

    // No session behind the newer position: no device to name. Episodes have
    // no chapters, so times.
    @Test
    fun `an episode moved with no session says elsewhere, in times`() {
        assertEquals(
            ResumeWording(
                headline = "This episode moved on elsewhere",
                heard = "Just now · 25 min there, 15 min here",
                hereMark = "here · 15 min",
                thereMark = "elsewhere · 25 min",
                continueLabel = "Continue from 25 min",
                continueDetail = "the newer position",
                stayLabel = "Stay at 15 min",
                stayDetail = "where this TV left off"
            ),
            words(offer(here = 900.0, there = 1500.0, ago = 20_000, device = null), episode)
        )
    }

    @Test
    fun `a book moved with no session says elsewhere, and keeps its chapters`() {
        val words = words(offer(device = null))

        assertEquals("This book moved on elsewhere", words.headline)
        assertEquals("elsewhere · Chapter 12", words.thereMark)
        assertEquals("1 h 20 min · the newer position", words.continueDetail)
    }

    // The newer position can be behind this one: it was not "further".
    @Test
    fun `a newer position behind this one went back`() {
        assertEquals("You went back on iPhone", words(offer(here = 4800.0, there = 3900.0)).headline)
        assertEquals("This book went back elsewhere", words(offer(here = 4800.0, there = 3900.0, device = null)).headline)
        assertEquals("This episode went back elsewhere", words(offer(here = 1500.0, there = 900.0, device = null), episode).headline)
    }

    @Test
    fun `a book without chapters uses times`() {
        val words = words(item = book.copy(chapters = emptyList()))

        assertEquals("Continue from 1 h 20 min", words.continueLabel)
        assertEquals("where iPhone left off", words.continueDetail)
        assertEquals("Stay at 1 h 05 min", words.stayLabel)
        assertEquals("here · 1 h 05 min", words.hereMark)
    }

    // "Continue from Chapter 8" and "Stay at Chapter 8" would not say which is which.
    @Test
    fun `two places in one chapter use times`() {
        val words = words(offer(here = 3650.0, there = 4100.0))

        assertEquals("Continue from 1 h 08 min", words.continueLabel)
        assertEquals("Stay at 1 h 00 min", words.stayLabel)
        assertEquals("iPhone · 1 h 08 min", words.thereMark)
    }

    // Audiobookshelf names an Android device "manufacturer model": no "your"
    // or "the" around a name of unknown shape.
    @Test
    fun `a device named by maker and model reads as it is`() {
        val words = words(offer(device = "Google Pixel 8"))

        assertEquals("You listened further on Google Pixel 8", words.headline)
        assertEquals("1 h 20 min · where Google Pixel 8 left off", words.continueDetail)
    }

    // More than 30 s apart, yet in one minute: the minutes alone would match.
    @Test
    fun `two places in one minute say the seconds`() {
        val words = words(offer(here = 3601.0, there = 3635.0))

        assertEquals("Continue from 1 h 00 min 35 s", words.continueLabel)
        assertEquals("Stay at 1 h 00 min 01 s", words.stayLabel)
        assertEquals("Just now · 15 min 50 s there, 15 min 10 s here", words(offer(here = 910.0, there = 950.0, ago = 0), episode).heard)
    }

    @Test
    fun `under an hour, minutes only`() {
        assertEquals("10 minutes ago · 52 min there, 45 min here", words(offer(here = 2700.0, there = 3120.0), episode).heard)
    }

    @Test
    fun `when it was heard reads naturally`() {
        val ago = { millis: Long -> words(offer(ago = millis)).heard.substringBefore(" · ") }

        assertEquals("Just now", ago(20_000))
        assertEquals("1 minute ago", ago(minute))
        assertEquals("1 hour ago", ago(61 * minute))
        assertEquals("3 hours ago", ago(3 * 60 * minute + 5 * minute))
        assertEquals("2 days ago", ago(2 * 24 * 60 * minute))
    }

    @Test
    fun `the card says it, with a button for each place`() {
        show()
        val words = words()

        assertEquals(words.headline, text("resume_headline"))
        assertEquals(words.heard, text("resume_heard"))
        assertEquals(words.hereMark, text("resume_here_label"))
        assertEquals(words.thereMark, text("resume_there_label"))
        assertEquals("${words.continueLabel} ${words.continueDetail}", text("resume_continue"))
        assertEquals("${words.stayLabel} ${words.stayDetail}", text("resume_stay"))
    }

    // Centre of each mark over the line, as a share of the item's length.
    @Test
    fun `both marks sit on the timeline by position`() {
        show(offer(here = 1800.0, there = 5400.0))
        val line = compose.onNodeWithTag("resume_line").fetchSemanticsNode().boundsInRoot
        val centre = { tag: String -> compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot.center.x }

        assertEquals(line.left + line.width * 0.25f, centre("resume_mark_here"), 1.5f)
        assertEquals(line.left + line.width * 0.75f, centre("resume_mark_there"), 1.5f)
    }

    @Test
    fun `a place past the end sits at the end`() {
        show(offer(here = 1800.0, there = 9000.0))
        val line = compose.onNodeWithTag("resume_line").fetchSemanticsNode().boundsInRoot

        assertEquals(line.right, compose.onNodeWithTag("resume_mark_there").fetchSemanticsNode().boundsInRoot.center.x, 1.5f)
    }

    // Sized like the transport: its Play is 60 dp.
    @Test
    fun `the buttons are as tall as the transport's Play`() {
        show()

        compose.onNodeWithTag("resume_continue").assertHeightIsAtLeast(60.dp)
        compose.onNodeWithTag("resume_stay").assertHeightIsAtLeast(60.dp)
    }

    // A focused TV button grows by a tenth about its centre (#209).
    @Test
    fun `a focused answer, grown, stays clear of the other`() {
        show()
        val continueBounds = compose.onNodeWithTag("resume_continue").fetchSemanticsNode().boundsInRoot
        val stayBounds = compose.onNodeWithTag("resume_stay").fetchSemanticsNode().boundsInRoot

        val clear = stayBounds.left - continueBounds.right - continueBounds.width * 0.05f
        assertTrue("only ${clear / compose.density.density} dp clear", clear >= with(compose.density) { 8.dp.toPx() })
    }

    @Test
    fun `focus starts on the server's position`() {
        show()

        compose.onNodeWithTag("resume_continue").assertIsFocused()
    }

    @Test
    fun `the remote's centre on it continues from there`() {
        show()

        compose.onNodeWithTag("resume_continue").performKeyInput { pressKey(Key.DirectionCenter) }
        compose.waitForIdle()

        assertEquals("there", chosen)
    }

    // The question holds the D-pad until it is answered.
    @Test
    fun `the D-pad does not leave the question`() {
        show()

        for (key in listOf(Key.DirectionDown, Key.DirectionUp, Key.DirectionLeft)) {
            compose.onNodeWithTag("resume_continue").performKeyInput { pressKey(key) }
            compose.waitForIdle()
            compose.onNodeWithTag("behind").assertIsNotFocused()
        }
    }

    @Test
    fun `Back stays`() {
        show()

        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()

        assertEquals("here", chosen)
    }

    @Test
    fun `staying is the other button`() {
        show()

        compose.onNodeWithTag("resume_stay").performSemanticsAction(SemanticsActions.RequestFocus)
        compose.onNodeWithTag("resume_stay").performKeyInput { pressKey(Key.DirectionCenter) }
        compose.waitForIdle()

        assertEquals("here", chosen)
    }
}
