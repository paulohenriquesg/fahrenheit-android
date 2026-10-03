package com.paulohenriquesg.fahrenheit.login

import androidx.activity.ComponentActivity
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.assert
import androidx.compose.runtime.MutableState
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * After a sign-out the app still knows the server and the username (#63), so
 * a return visit asks for the one thing missing: the password (#100). The form
 * sits in the top half, clear of the keyboard that owns the bottom 45%.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w960dp-h540dp")
// Real text metrics: the keyboard check is about heights.
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LoginReturningTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private var login: Triple<String, String, String>? = null
    private var logins = 0
    private var loadingOnSubmit = false
    private var apiKey: Pair<String, String>? = null

    private fun remember(host: String, username: String) {
        val prefs = SharedPreferencesHandler(compose.activity)
        prefs.saveUserPreferences(prefs.getUserPreferences().copy(host = host, username = username))
    }

    private fun show(error: LoginError? = null) {
        compose.setContent {
            FahrenheitTheme {
                LoginScreen(
                    { h, u, p, loading: MutableState<Boolean> ->
                        login = Triple(h, u, p)
                        logins++
                        if (loadingOnSubmit) loading.value = true
                    },
                    { h, k, _ -> apiKey = h to k },
                    error = error
                )
            }
        }
        compose.waitForIdle()
    }

    // A TV button answers the centre key, not a tap.
    private fun press(tag: String) {
        compose.onNodeWithTag(tag).performSemanticsAction(SemanticsActions.RequestFocus)
        compose.onNodeWithTag(tag).performKeyInput { pressKey(Key.DirectionCenter) }
        compose.waitForIdle()
    }

    private fun editable(tag: String) = compose.onNodeWithTag(tag).fetchSemanticsNode()
        .config[SemanticsProperties.EditableText].text

    @Test
    fun `a returning user is welcomed and asked only for the password`() {
        remember("http://abs.local:13378", "someone")
        show()

        compose.onNodeWithText(compose.activity.getString(R.string.login_welcome_back)).assertExists()
        compose.onNodeWithTag("login_account")
            .assertTextContains("someone", substring = true)
            .assertTextContains("abs.local:13378", substring = true)
        compose.onNodeWithTag("login_host_field").assertDoesNotExist()
        compose.onNodeWithTag("login_username_field").assertDoesNotExist()
        compose.onNodeWithTag("login_password_field").assertExists()
    }

    @Test
    fun `the address is shown as a fact, without its scheme`() {
        remember("https://abs.local/", "someone")
        show()

        val text = compose.onNodeWithTag("login_account").fetchSemanticsNode()
            .config[SemanticsProperties.Text].joinToString(" ") { it.text }
        assertTrue(text, !text.contains("https://"))
        assertTrue(text, text.contains("abs.local"))
        assertTrue(text, !text.endsWith("/"))
    }

    @Test
    fun `finishing the password signs in as the remembered user`() {
        remember("http://abs.local:13378", "someone")
        show()

        compose.onNodeWithTag("login_password_field").performTextInput("hunter2")
        compose.onNodeWithTag("login_password_field").performImeAction()

        assertEquals(Triple("http://abs.local:13378", "someone", "hunter2"), login)
    }

    @Test
    fun `sign in uses the remembered user too`() {
        remember("http://abs.local:13378", "someone")
        show()

        compose.onNodeWithTag("login_password_field").performTextInput("hunter2")
        press("login_submit_button")

        assertEquals(Triple("http://abs.local:13378", "someone", "hunter2"), login)
    }

    @Test
    fun `an API key can be used instead, on the same server`() {
        remember("http://abs.local:13378", "someone")
        show()

        press("login_mode_toggle")
        compose.onNodeWithTag("login_password_field").assertDoesNotExist()
        compose.onNodeWithTag("login_host_field").assertDoesNotExist()
        compose.onNodeWithTag("login_api_key_field").performTextInput("abc123")
        compose.onNodeWithTag("login_api_key_field").performImeAction()

        assertEquals("http://abs.local:13378" to "abc123", apiKey)
    }

    @Test
    fun `a different server opens the full form, still filled in`() {
        remember("http://abs.local:13378", "someone")
        show()

        press("login_different_server")

        assertEquals("http://abs.local:13378", editable("login_host_field"))
        assertEquals("someone", editable("login_username_field"))
        compose.onNodeWithTag("login_account").assertDoesNotExist()
    }

    @Test
    fun `the form and its buttons sit above the keyboard`() {
        remember("http://abs.local:13378", "someone")
        show()

        // The Fire TV keyboard covers the bottom 45%.
        val keyboardTop = 540.dp * 0.55f
        for (tag in listOf("login_password_field", "login_submit_button", "login_different_server")) {
            val bottom = compose.onNodeWithTag(tag).getUnclippedBoundsInRoot().bottom
            assertTrue("$tag ends at $bottom, under the keyboard ($keyboardTop)", bottom <= keyboardTop)
        }
    }

    @Test
    fun `an error shows above the password, even one about the address`() {
        remember("http://abs.local:13378", "someone")
        show(LoginError.Unreachable)

        val account = compose.onNodeWithTag("login_account").getUnclippedBoundsInRoot()
        val error = compose.onNodeWithTag("login_error").getUnclippedBoundsInRoot()
        val field = compose.onNodeWithTag("login_password_field").getUnclippedBoundsInRoot()
        assertTrue("error (${error.top}) should start below the account (${account.bottom})", account.bottom <= error.top)
        assertTrue("error (${error.bottom}) should end above the field (${field.top})", error.bottom <= field.top)
    }

    @Test
    fun `a remembered host without a username is a first run`() {
        remember("http://abs.local:13378", "")
        show()

        compose.onNodeWithTag("login_host_field").assertExists()
        compose.onNodeWithTag("login_username_field").assertExists()
        compose.onNodeWithTag("login_account").assertDoesNotExist()
    }

    private fun bounds(tag: String) = compose.onNodeWithTag(tag).getUnclippedBoundsInRoot()

    // The card merges its lines for a screen reader; the test reads them apart.
    private fun text(tag: String) = compose.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode()
        .config[SemanticsProperties.Text].joinToString(" ") { it.text }

    // Accessibility services click; the remote's centre key is covered by
    // `sign in uses the remembered user too`.
    @Test
    fun `Sign in submits when clicked by an accessibility service`() {
        remember("http://abs.local:13378", "someone")
        show()

        compose.onNodeWithTag("login_password_field").performTextInput("hunter2")
        compose.onNodeWithTag("login_submit_button").performSemanticsAction(SemanticsActions.OnClick)

        assertEquals(Triple("http://abs.local:13378", "someone", "hunter2"), login)
    }

    // On a Fire TV remote Play/Pause is the field's Enter: it confirms the
    // host and username. In the last field it did nothing at all.
    @Test
    fun `Play on the remote in the password field signs in`() {
        remember("http://abs.local:13378", "someone")
        show()

        compose.onNodeWithTag("login_password_field").performTextInput("hunter2")
        compose.onNodeWithTag("login_password_field").performKeyInput { pressKey(Key.MediaPlayPause) }

        assertEquals(Triple("http://abs.local:13378", "someone", "hunter2"), login)
        assertEquals(1, logins)
    }

    @Test
    fun `Play on the remote in the API key field signs in with it`() {
        remember("http://abs.local:13378", "someone")
        show()
        press("login_mode_toggle")

        compose.onNodeWithTag("login_api_key_field").performTextInput("abc123")
        compose.onNodeWithTag("login_api_key_field").performKeyInput { pressKey(Key.MediaPlayPause) }

        assertEquals("http://abs.local:13378" to "abc123", apiKey)
    }

    // The spinner used to replace the buttons, which took focus with them:
    // on the stick, signing in looked like nothing happening.
    @Test
    fun `while signing in, Sign in shows progress and keeps focus`() {
        remember("http://abs.local:13378", "someone")
        loadingOnSubmit = true
        show()

        press("login_submit_button")

        compose.onNodeWithTag("login_submit_button")
            .assertIsFocused()
            .assert(hasAnyDescendant(hasTestTag("login_progress")))
        press("login_submit_button")
        assertEquals("a second press does not send a second request", 1, logins)
    }

    @Test
    fun `the first-run Login button shows progress too`() {
        loadingOnSubmit = true
        show()

        press("login_submit_button")

        compose.onNodeWithTag("login_submit_button")
            .assertIsFocused()
            .assert(hasAnyDescendant(hasTestTag("login_progress")))
    }

    // Frame 3: a large title on the left, the account as a card below it.
    @Test
    fun `title, account card and field line up on the left`() {
        remember("http://abs.local:13378", "someone")
        show()

        val title = bounds("login_title")
        val card = bounds("login_account")
        val field = bounds("login_password_field")
        assertEquals(field.left.value, title.left.value, 1f)
        assertEquals(field.left.value, card.left.value, 1f)
    }

    @Test
    fun `the account card has the name, and the server under it`() {
        remember("http://abs.local:13378", "someone")
        show()

        assertEquals("someone", text("login_account_name"))
        assertTrue(text("login_account_server").contains("abs.local:13378"))
        val name = compose.onNodeWithTag("login_account_name", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val server = compose.onNodeWithTag("login_account_server", useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue(server.top >= name.bottom)
    }

    // On the stick, Down from the field left no button looking focused.
    @Test
    fun `Down from the password field lands on Sign in, and Right walks the buttons`() {
        remember("http://abs.local:13378", "someone")
        show()
        compose.onNodeWithTag("login_password_field").performSemanticsAction(SemanticsActions.RequestFocus)
        compose.onNodeWithTag("login_password_field").assertIsFocused()

        compose.onNodeWithTag("login_password_field").performKeyInput { pressKey(Key.DirectionDown) }
        compose.waitForIdle()
        compose.onNodeWithTag("login_submit_button").assertIsFocused()

        compose.onNodeWithTag("login_submit_button").performKeyInput { pressKey(Key.DirectionRight) }
        compose.waitForIdle()
        compose.onNodeWithTag("login_mode_toggle").assertIsFocused()

        compose.onNodeWithTag("login_mode_toggle").performKeyInput { pressKey(Key.DirectionRight) }
        compose.waitForIdle()
        compose.onNodeWithTag("login_different_server").assertIsFocused()
    }
}
