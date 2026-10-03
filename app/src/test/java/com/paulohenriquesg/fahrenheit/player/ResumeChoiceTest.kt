package com.paulohenriquesg.fahrenheit.player

import androidx.activity.ComponentActivity
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import com.paulohenriquesg.fahrenheit.R
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The question (#90): "You're at 1 h 05 min here. Continue from 1 h 20 min,
 * listened to 10 minutes ago on another device?" - two buttons, focus on the
 * server's position.
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

    private fun offer(here: Double = 3900.0, there: Double = 4800.0, ago: Long = 10 * minute) =
        ResumeOffer(here = here, there = there, listenedAt = now - ago)

    private fun question(offer: ResumeOffer) = resumeQuestion(compose.activity.resources, offer, now)

    private fun show(offer: ResumeOffer = offer()) {
        compose.setContent {
            FahrenheitTheme {
                ResumeChoice(offer, now, onContinue = { chosen = "there" }, onStay = { chosen = "here" })
            }
        }
        compose.waitForIdle()
    }

    private fun text(tag: String) = compose.onNodeWithTag(tag).fetchSemanticsNode()
        .config[SemanticsProperties.Text].joinToString(" ") { it.text }

    @Test
    fun `the question names both positions and when the other was heard`() {
        assertEquals(
            "You're at 1 h 05 min here. Continue from 1 h 20 min, listened to 10 minutes ago on another device?",
            question(offer())
        )
    }

    @Test
    fun `under an hour, minutes only`() {
        assertEquals(
            "You're at 45 min here. Continue from 52 min, listened to 10 minutes ago on another device?",
            question(offer(here = 2700.0, there = 3120.0))
        )
    }

    @Test
    fun `when it was heard reads naturally`() {
        val ago = { millis: Long -> question(offer(ago = millis)).substringAfter("listened to ").substringBefore(" on another") }

        assertEquals("just now", ago(20_000))
        assertEquals("1 minute ago", ago(minute))
        assertEquals("1 hour ago", ago(61 * minute))
        assertEquals("3 hours ago", ago(3 * 60 * minute + 5 * minute))
        assertEquals("2 days ago", ago(2 * 24 * 60 * minute))
    }

    @Test
    fun `the screen asks the question, with a button for each position`() {
        show()

        assertEquals(question(offer()), text("resume_question"))
        assertEquals(compose.activity.getString(R.string.resume_continue_from, "1 h 20 min"), text("resume_continue"))
        assertEquals(compose.activity.getString(R.string.resume_stay_at, "1 h 05 min"), text("resume_stay"))
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

    @Test
    fun `staying is the other button`() {
        show()

        compose.onNodeWithTag("resume_stay").performSemanticsAction(SemanticsActions.RequestFocus)
        compose.onNodeWithTag("resume_stay").performKeyInput { pressKey(Key.DirectionCenter) }
        compose.waitForIdle()

        assertEquals("here", chosen)
    }
}
