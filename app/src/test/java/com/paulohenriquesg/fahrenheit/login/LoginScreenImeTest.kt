package com.paulohenriquesg.fahrenheit.login

import androidx.activity.ComponentActivity
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * What the login fields accept, and how a TV keyboard gets out of them.
 *
 * Found on a Fire TV Stick (issue #51): the host field swallowed a newline,
 * and the password field's keyboard offered only "Next", which moved focus
 * behind the still-open keyboard. The Login button sits under that keyboard
 * and nothing on the remote dismisses it, so sign-in was a dead end.
 */
@OptIn(ExperimentalTestApi::class, ExperimentalComposeUiApi::class)
@RunWith(RobolectricTestRunner::class)
class LoginScreenImeTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun textOf(tag: String): String =
        compose.onNodeWithTag(tag).fetchSemanticsNode()
            .config[SemanticsProperties.EditableText].text

    // A hardware Enter - what a remote or keyboard sends - used to land in the
    // value as a line break, and a host with a newline in it is not a URL.
    @Test
    fun `Enter in the host field does not become a line break`() {
        compose.setContent { LoginScreen({ _, _, _, _ -> }, { _, _, _ -> }) }
        compose.onNodeWithTag("login_host_field").performTextInput("http://abs.local:13378")

        compose.onNodeWithTag("login_host_field").performKeyInput { pressKey(Key.Enter) }

        assertEquals("http://abs.local:13378", textOf("login_host_field"))
    }

    @Test
    fun `finishing the password signs in`() {
        var submitted: Triple<String, String, String>? = null
        compose.setContent {
            LoginScreen({ host, user, pass, _ -> submitted = Triple(host, user, pass) }, { _, _, _ -> })
        }
        compose.onNodeWithTag("login_host_field").performTextInput("http://abs.local:13378")
        compose.onNodeWithTag("login_username_field").performTextInput("someone")
        compose.onNodeWithTag("login_password_field").performTextInput("hunter2")

        compose.onNodeWithTag("login_password_field").performImeAction()

        assertEquals(Triple("http://abs.local:13378", "someone", "hunter2"), submitted)
    }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun `finishing the api key signs in with it`() {
        var key: String? = null
        compose.setContent { LoginScreen({ _, _, _, _ -> }, { _, k, _ -> key = k }) }
        // A TV button answers the centre key, not a tap.
        compose.onNodeWithTag("login_mode_toggle")
            .performSemanticsAction(SemanticsActions.RequestFocus)
        compose.onNodeWithTag("login_mode_toggle")
            .performKeyInput { pressKey(Key.DirectionCenter) }
        compose.waitForIdle()
        compose.onNodeWithTag("login_host_field").performTextInput("http://abs.local:13378")
        compose.onNodeWithTag("login_api_key_field").performTextInput("abc123")

        compose.onNodeWithTag("login_api_key_field").performImeAction()

        assertEquals("abc123", key)
    }

    @Test
    fun `Enter in the username field does not become a line break`() {
        compose.setContent { LoginScreen({ _, _, _, _ -> }, { _, _, _ -> }) }
        compose.onNodeWithTag("login_username_field").performTextInput("someone")

        compose.onNodeWithTag("login_username_field").performKeyInput { pressKey(Key.Enter) }

        assertTrue(!textOf("login_username_field").contains("\n"))
    }
}
