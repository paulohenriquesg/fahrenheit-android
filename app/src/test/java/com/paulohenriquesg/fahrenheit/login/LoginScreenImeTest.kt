package com.paulohenriquesg.fahrenheit.login

import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
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

    // The address is its own step on first run (#132).
    private fun enterAddress() {
        compose.onNodeWithTag("login_host_field").performTextInput("http://abs.local:13378")
        compose.onNodeWithTag("login_host_field").performImeAction()
        compose.waitForIdle()
    }

    // A hardware Enter - what a remote or keyboard sends - used to land in the
    // value as a line break, and a host with a newline in it is not a URL. Now
    // it finishes the address and moves on to signing in.
    @Test
    fun `Enter in the host field does not become a line break`() {
        var submitted: Triple<String, String, String>? = null
        compose.setContent {
            LoginScreen({ host, user, pass, _ -> submitted = Triple(host, user, pass) }, { _, _, _ -> })
        }
        compose.onNodeWithTag("login_host_field").performTextInput("http://abs.local:13378")

        compose.onNodeWithTag("login_host_field").performKeyInput { pressKey(Key.Enter) }
        compose.waitForIdle()
        compose.onNodeWithTag("login_username_field").performTextInput("someone")
        compose.onNodeWithTag("login_password_field").performImeAction()

        assertEquals("http://abs.local:13378", submitted?.first)
    }

    @Test
    fun `finishing the password signs in`() {
        var submitted: Triple<String, String, String>? = null
        compose.setContent {
            LoginScreen({ host, user, pass, _ -> submitted = Triple(host, user, pass) }, { _, _, _ -> })
        }
        enterAddress()
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
        enterAddress()
        // A TV button answers the centre key, not a tap.
        compose.onNodeWithTag("login_mode_toggle")
            .performSemanticsAction(SemanticsActions.RequestFocus)
        compose.onNodeWithTag("login_mode_toggle")
            .performKeyInput { pressKey(Key.DirectionCenter) }
        compose.waitForIdle()
        compose.onNodeWithTag("login_api_key_field").performTextInput("abc123")

        compose.onNodeWithTag("login_api_key_field").performImeAction()

        assertEquals("abc123", key)
    }

    @Test
    fun `Enter in the username field does not become a line break`() {
        compose.setContent { LoginScreen({ _, _, _, _ -> }, { _, _, _ -> }) }
        enterAddress()
        compose.onNodeWithTag("login_username_field").performTextInput("someone")

        compose.onNodeWithTag("login_username_field").performKeyInput { pressKey(Key.Enter) }

        assertTrue(!textOf("login_username_field").contains("\n"))
    }

    private fun showApiKey() {
        compose.onNodeWithTag("login_mode_toggle")
            .performSemanticsAction(SemanticsActions.RequestFocus)
        compose.onNodeWithTag("login_mode_toggle")
            .performKeyInput { pressKey(Key.DirectionCenter) }
        compose.waitForIdle()
    }

    // What the field asks the keyboard for, read where the keyboard reads it.
    private fun editorInfoOf(tag: String): EditorInfo {
        compose.onNodeWithTag(tag).performTextInput("x")
        val info = EditorInfo()
        compose.runOnUiThread {
            val root = compose.activity.findViewById<ViewGroup>(android.R.id.content).getChildAt(0)
            val editor = generateSequence<View>(root) { (it as? ViewGroup)?.getChildAt(0) }
                .first { it.onCheckIsTextEditor() }
            editor.onCreateInputConnection(info)
        }
        return info
    }

    // Masking the field on screen is not enough (#163): the Fire TV keyboard
    // draws what is typed in its own preview line, in clear, unless the field
    // says it is a password - and may keep it for its suggestions.
    private fun assertSecret(info: EditorInfo) {
        assertEquals(InputType.TYPE_CLASS_TEXT, info.inputType and InputType.TYPE_MASK_CLASS)
        assertEquals(
            InputType.TYPE_TEXT_VARIATION_PASSWORD,
            info.inputType and InputType.TYPE_MASK_VARIATION
        )
        assertEquals(0, info.inputType and InputType.TYPE_TEXT_FLAG_AUTO_CORRECT)
        assertEquals(0, info.inputType and InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE)
    }

    @Test
    fun `the password field tells the keyboard it is a password`() {
        compose.setContent { LoginScreen({ _, _, _, _ -> }, { _, _, _ -> }) }
        enterAddress()

        val info = editorInfoOf("login_password_field")

        assertSecret(info)
        assertEquals(EditorInfo.IME_ACTION_DONE, info.imeOptions and EditorInfo.IME_MASK_ACTION)
    }

    @Test
    fun `the api key field tells the keyboard it is a password`() {
        compose.setContent { LoginScreen({ _, _, _, _ -> }, { _, _, _ -> }) }
        enterAddress()
        showApiKey()

        val info = editorInfoOf("login_api_key_field")

        assertSecret(info)
        assertEquals(EditorInfo.IME_ACTION_DONE, info.imeOptions and EditorInfo.IME_MASK_ACTION)
    }
}
