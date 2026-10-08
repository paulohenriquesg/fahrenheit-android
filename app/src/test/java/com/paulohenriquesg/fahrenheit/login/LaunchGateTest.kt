package com.paulohenriquesg.fahrenheit.login

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
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
 * while the session is checked, then Home, or the sign-in form as before.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w960dp-h540dp")
class LaunchGateTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val host = "http://abs.local:13378"
    private var wentHome = 0
    private var signInErrors = mutableListOf<LoginError?>()
    private val answer = CompletableDeferred<SessionCheck.Result>()

    @Before
    fun rememberSession() {
        val prefs = SharedPreferencesHandler(compose.activity)
        prefs.saveUserPreferences(prefs.getUserPreferences().copy(host = host, username = "someone", token = "t"))
    }

    private fun show(check: (suspend () -> SessionCheck.Result)? = { answer.await() }) {
        // The status line changes on a timer; the test moves the clock.
        compose.mainClock.autoAdvance = false
        compose.setContent {
            FahrenheitTheme {
                LaunchGate(
                    check = check,
                    onReady = { wentHome++ },
                    onSignIn = { signInErrors += it }
                ) {
                    LoginScreen({ _, _, _, _ -> }, { _, _, _ -> })
                }
            }
        }
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
    }

    private fun answer(result: SessionCheck.Result) {
        answer.complete(result)
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
    }

    @Test
    fun `while the session is checked, the name and Signing in show, not the form`() {
        show()

        compose.onNodeWithTag("launch_brand").assertTextEquals("Fahrenheit")
        compose.onNodeWithTag("launch_status").assertTextEquals("Signing in…")
        compose.onAllNodesWithTag("login_screen").assertCountEquals(0)
    }

    @Test
    fun `a check that takes a while says it is connecting to the server`() {
        show()

        compose.mainClock.advanceTimeBy(2_900)
        compose.onNodeWithTag("launch_status").assertTextEquals("Signing in…")
        compose.mainClock.advanceTimeBy(200)
        compose.onNodeWithTag("launch_status").assertTextEquals("Connecting to your server…")
    }

    @Test
    fun `the server address is never on the launch screen`() {
        show()
        compose.mainClock.advanceTimeBy(5_000)

        compose.onAllNodes(hasText("abs.local", substring = true)).assertCountEquals(0)
    }

    @Test
    fun `a good session goes Home once, and the form never draws`() {
        show()
        answer(SessionCheck.Result.Ready)

        assertEquals(1, wentHome)
        assertEquals(emptyList<LoginError?>(), signInErrors)
        compose.onAllNodesWithTag("login_screen").assertCountEquals(0)
    }

    @Test
    fun `a session that does not work brings the sign-in form, with why`() {
        show()
        answer(SessionCheck.Result.SignIn(LoginError.Unreachable))

        assertEquals(0, wentHome)
        assertEquals(listOf<LoginError?>(LoginError.Unreachable), signInErrors)
        compose.onNodeWithTag("login_screen").assertExists()
        compose.onAllNodesWithTag("launch_screen").assertCountEquals(0)
    }

    @Test
    fun `with no session to check, the form shows straight away`() {
        show(check = null)

        assertEquals(0, wentHome)
        assertEquals(emptyList<LoginError?>(), signInErrors)
        compose.onNodeWithTag("login_screen").assertExists()
        compose.onAllNodesWithTag("launch_screen").assertCountEquals(0)
    }
}
