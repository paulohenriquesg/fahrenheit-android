package com.paulohenriquesg.fahrenheit.login

import androidx.activity.ComponentActivity
import androidx.compose.runtime.MutableState
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.dp
import com.paulohenriquesg.fahrenheit.R
import com.paulohenriquesg.fahrenheit.storage.SharedPreferencesHandler
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Frame 2 of the login mock (#132): on first run the address is asked for on
 * its own, then the sign-in form names the server as a fact - "Sign in to
 * abs.local" - and asks only for the username and password, all of it in the
 * top half, above the Fire TV keyboard.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w960dp-h540dp")
// Real text metrics: the keyboard checks are about heights.
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LoginFirstRunTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private var login: Triple<String, String, String>? = null
    private var apiKey: Pair<String, String>? = null
    private var dismissed = 0
    private var loadingOnSubmit = false
    private var loadingState: MutableState<Boolean>? = null

    // A sign-in left loading keeps its spinner animating after the test, and
    // that endless animation left later Compose test classes never idle.
    @After
    fun stopLoading() {
        compose.runOnUiThread { loadingState?.value = false }
        compose.waitForIdle()
    }

    private fun show(error: LoginError? = null) {
        compose.setContent {
            FahrenheitTheme {
                LoginScreen(
                    { h, u, p, loading ->
                        login = Triple(h, u, p)
                        if (loadingOnSubmit) loading.value = true
                        loadingState = loading
                    },
                    { h, k, _ -> apiKey = h to k },
                    error = error,
                    onDismissError = { dismissed++ }
                )
            }
        }
        compose.waitForIdle()
    }

    private fun press(tag: String) {
        compose.onNodeWithTag(tag).performSemanticsAction(SemanticsActions.RequestFocus)
        compose.onNodeWithTag(tag).performKeyInput { pressKey(Key.DirectionCenter) }
        compose.waitForIdle()
    }

    private fun text(tag: String) = compose.onNodeWithTag(tag).fetchSemanticsNode()
        .config[SemanticsProperties.Text].joinToString(" ") { it.text }

    private fun editable(tag: String) = compose.onNodeWithTag(tag).fetchSemanticsNode()
        .config[SemanticsProperties.EditableText].text

    private fun enterAddress(host: String) {
        compose.onNodeWithTag("login_host_field").performTextInput(host)
        press("login_continue")
    }

    @Test
    fun `a first run asks for the address on its own`() {
        show()

        assertEquals(compose.activity.getString(R.string.login_where_is_library), text("login_title"))
        compose.onNodeWithTag("login_host_field").assertExists()
        compose.onNodeWithTag("login_username_field").assertDoesNotExist()
        compose.onNodeWithTag("login_password_field").assertDoesNotExist()
    }

    @Test
    fun `after the address, the server is a fact and only username and password are asked`() {
        show()

        enterAddress("http://abs.local:13378")

        assertEquals(
            compose.activity.getString(R.string.login_sign_in_to, "abs.local:13378"),
            text("login_title")
        )
        compose.onNodeWithTag("login_host_field").assertDoesNotExist()
        compose.onNodeWithTag("login_username_field").assertExists()
        compose.onNodeWithTag("login_password_field").assertExists()
    }

    @Test
    fun `the keyboard's Done in the address continues`() {
        show()

        compose.onNodeWithTag("login_host_field").performTextInput("http://abs.local:13378")
        compose.onNodeWithTag("login_host_field").performImeAction()
        compose.waitForIdle()

        compose.onNodeWithTag("login_username_field").assertExists()
    }

    @Test
    fun `Play on the remote in the address continues`() {
        show()

        compose.onNodeWithTag("login_host_field").performTextInput("http://abs.local:13378")
        compose.onNodeWithTag("login_host_field").performKeyInput { pressKey(Key.MediaPlayPause) }
        compose.waitForIdle()

        compose.onNodeWithTag("login_username_field").assertExists()
    }

    // Checked here, before asking for anything else: nothing is sent.
    @Test
    fun `an address without a scheme is reported above the address and goes no further`() {
        show()

        enterAddress("abs.local:13378")

        compose.onNodeWithTag("login_username_field").assertDoesNotExist()
        assertEquals(compose.activity.getString(R.string.login_error_host_scheme), text("login_error"))
        val error = compose.onNodeWithTag("login_error").getUnclippedBoundsInRoot()
        val field = compose.onNodeWithTag("login_host_field").getUnclippedBoundsInRoot()
        assertTrue(error.bottom <= field.top)
    }

    @Test
    fun `a blank address is reported too`() {
        show()

        press("login_continue")

        assertEquals(compose.activity.getString(R.string.login_error_host_missing), text("login_error"))
    }

    @Test
    fun `finishing the password signs in to that server`() {
        show()
        enterAddress("http://abs.local:13378")

        compose.onNodeWithTag("login_username_field").performTextInput("someone")
        compose.onNodeWithTag("login_password_field").performTextInput("hunter2")
        compose.onNodeWithTag("login_password_field").performImeAction()

        assertEquals(Triple("http://abs.local:13378", "someone", "hunter2"), login)
    }

    @Test
    fun `surrounding spaces in the address are not kept`() {
        show()
        enterAddress("  http://abs.local:13378 ")

        compose.onNodeWithTag("login_username_field").performTextInput("someone")
        compose.onNodeWithTag("login_password_field").performImeAction()

        assertEquals("http://abs.local:13378", login?.first)
    }

    @Test
    fun `an API key signs in to that server`() {
        show()
        enterAddress("http://abs.local:13378")

        press("login_mode_toggle")
        compose.onNodeWithTag("login_username_field").assertDoesNotExist()
        compose.onNodeWithTag("login_api_key_field").performTextInput("abc123")
        compose.onNodeWithTag("login_api_key_field").performImeAction()

        assertEquals("http://abs.local:13378" to "abc123", apiKey)
    }

    @Test
    fun `a different server goes back to the address, still filled in`() {
        show()
        enterAddress("http://abs.local:13378")

        press("login_different_server")

        assertEquals("http://abs.local:13378", editable("login_host_field"))
        compose.onNodeWithTag("login_username_field").assertDoesNotExist()
    }

    @Test
    fun `the sign-in form and its buttons sit above the keyboard`() {
        show()
        enterAddress("http://abs.local:13378")

        // The Fire TV keyboard covers the bottom 45%.
        val keyboardTop = 540.dp * 0.55f
        for (tag in listOf("login_username_field", "login_password_field", "login_submit_button")) {
            val bottom = compose.onNodeWithTag(tag).getUnclippedBoundsInRoot().bottom
            assertTrue("$tag ends at $bottom, under the keyboard ($keyboardTop)", bottom <= keyboardTop)
        }
    }

    @Test
    fun `the address step sits above the keyboard too`() {
        show()

        val keyboardTop = 540.dp * 0.55f
        for (tag in listOf("login_host_field", "login_continue")) {
            val bottom = compose.onNodeWithTag(tag).getUnclippedBoundsInRoot().bottom
            assertTrue("$tag ends at $bottom, under the keyboard ($keyboardTop)", bottom <= keyboardTop)
        }
    }

    @Test
    fun `a remembered address without a username goes straight to signing in`() {
        val prefs = SharedPreferencesHandler(compose.activity)
        prefs.saveUserPreferences(prefs.getUserPreferences().copy(host = "http://abs.local:13378", username = ""))
        show()

        compose.onNodeWithTag("login_host_field").assertDoesNotExist()
        compose.onNodeWithTag("login_username_field").assertExists()
        assertTrue(text("login_title").contains("abs.local:13378"))
    }

    private fun rememberHost() {
        val prefs = SharedPreferencesHandler(compose.activity)
        prefs.saveUserPreferences(prefs.getUserPreferences().copy(host = "http://abs.local:13378", username = ""))
    }

    // On a TV, focus that vanishes with the button that held it leaves the
    // remote doing nothing visible until a blind press.
    @Test
    fun `after the address, focus is on the username`() {
        show()

        enterAddress("http://abs.local:13378")

        compose.onNodeWithTag("login_username_field").assertIsFocused()
    }

    @Test
    fun `after the keyboard's Done in the address, focus is on the username`() {
        show()

        compose.onNodeWithTag("login_host_field").performTextInput("http://abs.local:13378")
        compose.onNodeWithTag("login_host_field").performImeAction()
        compose.waitForIdle()

        compose.onNodeWithTag("login_username_field").assertIsFocused()
    }

    @Test
    fun `after a different server, focus is on the address`() {
        show()
        enterAddress("http://abs.local:13378")

        press("login_different_server")

        compose.onNodeWithTag("login_host_field").assertIsFocused()
    }

    // The handler keeps its error until the next attempt; the last server's
    // "did not answer" must not greet the next one.
    @Test
    fun `moving between the address and signing in clears the last error`() {
        show()

        enterAddress("http://abs.local:13378")
        press("login_different_server")

        assertEquals(2, dismissed)
    }

    @Test
    fun `a password typed for one server is not sent to the next`() {
        show()
        enterAddress("http://abs.local:13378")
        compose.onNodeWithTag("login_password_field").performTextInput("hunter2")

        press("login_different_server")
        press("login_continue")

        assertEquals("", compose.onNodeWithTag("login_password_field").fetchSemanticsNode()
            .config[SemanticsProperties.EditableText].text)
    }

    @Test
    fun `the server cannot be changed while signing in`() {
        loadingOnSubmit = true
        show()
        enterAddress("http://abs.local:13378")
        compose.onNodeWithTag("login_password_field").performImeAction()

        press("login_different_server")

        compose.onNodeWithTag("login_username_field").assertExists()
    }

    // The address is not a field on this step; the way to change it is.
    @Test
    fun `a server that did not answer says how to change the address`() {
        rememberHost()
        show(LoginError.Unreachable)

        assertEquals(compose.activity.getString(R.string.login_error_unreachable_change), text("login_error"))
    }

    @Test
    fun `with an error showing, the sign-in step still fits above the keyboard`() {
        rememberHost()
        show(LoginError.UsernameMissing)

        val keyboardTop = 540.dp * 0.55f
        for (tag in listOf("login_password_field", "login_submit_button")) {
            val bottom = compose.onNodeWithTag(tag).getUnclippedBoundsInRoot().bottom
            assertTrue("$tag ends at $bottom, under the keyboard ($keyboardTop)", bottom <= keyboardTop)
        }
    }
}
