package com.paulohenriquesg.fahrenheit.login

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import com.paulohenriquesg.fahrenheit.storage.SharedPreferencesHandler
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Opening the app with a stored session (#191): the name and a status line
 * while the session is checked, then Home. Only the server rejecting the
 * session brings the sign-in form; no answer keeps the launch screen, with
 * Try again, retrying by itself.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w960dp-h540dp")
class LaunchGateTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val host = "http://abs.local:13378"
    private var wentHome = 0
    private var checks = 0

    /** Each check takes the next answer; one not given yet is still in flight. */
    private val answers = mutableListOf<CompletableDeferred<SessionCheck.Result>>()

    private val prefs by lazy { SharedPreferencesHandler(compose.activity) }

    @Before
    fun rememberSession() {
        prefs.saveUserPreferences(prefs.getUserPreferences().copy(host = host, username = "someone", token = "t"))
    }

    private fun answer(index: Int): CompletableDeferred<SessionCheck.Result> {
        while (answers.size <= index) answers += CompletableDeferred()
        return answers[index]
    }

    private fun show(withSession: Boolean = true) {
        // The status line and the retries run on timers; the test moves the clock.
        compose.mainClock.autoAdvance = false
        compose.setContent {
            FahrenheitTheme {
                LaunchGate(
                    check = if (withSession) {
                        { answer(checks++).await() }
                    } else null,
                    onReady = { wentHome++ }
                ) {
                    LoginScreen({ _, _, _, _ -> }, { _, _, _ -> })
                }
            }
        }
        settle()
    }

    private fun settle() {
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
    }

    private fun reply(index: Int, result: SessionCheck.Result) {
        answer(index).complete(result)
        settle()
    }

    private fun advance(ms: Long) {
        compose.mainClock.advanceTimeBy(ms)
        settle()
    }

    @Test
    fun `while the session is checked, the name and Signing in show, not the form`() {
        show()

        compose.onNodeWithTag("launch_brand").assertTextEquals("Fahrenheit")
        compose.onNodeWithTag("launch_status").assertTextEquals("Signing in…")
        compose.onAllNodesWithTag("login_screen").assertCountEquals(0)
        compose.onAllNodesWithTag("launch_try_again").assertCountEquals(0)
    }

    @Test
    fun `a check that takes a while says it is connecting to the server`() {
        show()

        advance(2_900)
        compose.onNodeWithTag("launch_status").assertTextEquals("Signing in…")
        advance(200)
        compose.onNodeWithTag("launch_status").assertTextEquals("Connecting to your server…")
    }

    @Test
    fun `a good session goes Home once, and the form never draws`() {
        show()
        reply(0, SessionCheck.Result.Ready)

        assertEquals(1, wentHome)
        compose.onAllNodesWithTag("login_screen").assertCountEquals(0)
    }

    @Test
    fun `a rejected session brings the sign-in form`() {
        show()
        reply(0, SessionCheck.Result.Rejected)

        assertEquals(0, wentHome)
        compose.onNodeWithTag("login_screen").assertExists()
        compose.onAllNodesWithTag("launch_screen").assertCountEquals(0)
    }

    @Test
    fun `no answer keeps the launch screen, saying so, with Try again focused`() {
        show()
        reply(0, SessionCheck.Result.Unreachable)

        compose.onAllNodesWithTag("login_screen").assertCountEquals(0)
        compose.onNodeWithTag("launch_status").assertTextEquals("Can't reach your server")
        compose.onNodeWithTag("launch_try_again").assertIsFocused()
    }

    @Test
    fun `the server address is never on the launch screen, reachable or not`() {
        show()
        advance(5_000)
        compose.onAllNodes(hasText("abs.local", substring = true)).assertCountEquals(0)

        reply(0, SessionCheck.Result.Unreachable)
        compose.onAllNodes(hasText("abs.local", substring = true)).assertCountEquals(0)
    }

    @Test
    fun `it tries again by itself after 2, 4 and 8 seconds, then waits for Try again`() {
        show()
        reply(0, SessionCheck.Result.Unreachable)

        advance(1_900)
        assertEquals(1, checks)
        advance(200)
        assertEquals(2, checks)
        reply(1, SessionCheck.Result.Unreachable)

        advance(3_900)
        assertEquals(2, checks)
        advance(200)
        assertEquals(3, checks)
        reply(2, SessionCheck.Result.Unreachable)

        advance(7_900)
        assertEquals(3, checks)
        advance(200)
        assertEquals(4, checks)
        reply(3, SessionCheck.Result.Unreachable)

        advance(60_000)
        assertEquals(4, checks)
        compose.onNodeWithTag("launch_try_again").assertIsFocused()
    }

    @Test
    fun `Try again asks at once`() {
        show()
        reply(0, SessionCheck.Result.Unreachable)

        compose.onNodeWithTag("launch_try_again").performSemanticsAction(SemanticsActions.OnClick)
        // Well short of the first 2 s wait.
        advance(100)

        assertEquals(2, checks)
    }

    @Test
    fun `a retry that gets through goes Home, and the session was kept all along`() {
        show()
        reply(0, SessionCheck.Result.Unreachable)
        advance(2_100)
        assertEquals("t", prefs.getUserPreferences().token)

        reply(1, SessionCheck.Result.Ready)

        assertEquals(1, wentHome)
        compose.onAllNodesWithTag("login_screen").assertCountEquals(0)
    }

    @Test
    fun `with no session to check, the form shows straight away`() {
        show(withSession = false)

        assertEquals(0, wentHome)
        assertEquals(0, checks)
        compose.onNodeWithTag("login_screen").assertExists()
        compose.onAllNodesWithTag("launch_screen").assertCountEquals(0)
    }
}
