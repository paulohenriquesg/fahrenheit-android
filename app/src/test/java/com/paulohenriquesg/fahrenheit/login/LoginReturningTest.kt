package com.paulohenriquesg.fahrenheit.login

import androidx.activity.ComponentActivity
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextContains
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

/**
 * After a sign-out the app still knows the server and the username (#63), so
 * a return visit asks for the one thing missing: the password (#100). The form
 * sits in the top half, clear of the keyboard that owns the bottom 45%.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w960dp-h540dp")
class LoginReturningTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private var login: Triple<String, String, String>? = null
    private var apiKey: Pair<String, String>? = null

    private fun remember(host: String, username: String) {
        val prefs = SharedPreferencesHandler(compose.activity)
        prefs.saveUserPreferences(prefs.getUserPreferences().copy(host = host, username = username))
    }

    private fun show(error: LoginError? = null) {
        compose.setContent {
            FahrenheitTheme {
                LoginScreen(
                    { h, u, p, _ -> login = Triple(h, u, p) },
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
}
