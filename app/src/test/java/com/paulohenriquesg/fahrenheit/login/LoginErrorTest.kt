package com.paulohenriquesg.fahrenheit.login

import androidx.activity.ComponentActivity
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import com.paulohenriquesg.fahrenheit.R
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A wrong password or a dead server used to be a Toast at the bottom of the
 * screen - under the Fire TV keyboard, which owns the bottom 45% while typing.
 * The message now sits directly above the field that caused it, and says what
 * to do next (#99).
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w960dp-h540dp")
class LoginErrorTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun show(error: LoginError?) {
        compose.setContent {
            FahrenheitTheme {
                LoginScreen({ _, _, _, _ -> }, { _, _, _ -> }, error = error)
            }
        }
        compose.waitForIdle()
    }

    private fun string(id: Int, vararg args: Any) = compose.activity.getString(id, *args)

    private fun assertDirectlyAbove(above: String, below: String) {
        val a = compose.onNodeWithTag(above).getUnclippedBoundsInRoot()
        val b = compose.onNodeWithTag(below).getUnclippedBoundsInRoot()
        assertTrue("$above (${a.bottom}) should end above $below (${b.top})", a.bottom <= b.top)
    }

    @Test
    fun `no error, no message`() {
        show(null)

        compose.onNodeWithTag("login_error").assertDoesNotExist()
    }

    @Test
    fun `a rejected password says what to do, right above the password field`() {
        show(LoginError.PasswordRejected)

        compose.onNodeWithTag("login_error")
            .assertTextContains(string(R.string.login_error_password_rejected))
        assertDirectlyAbove("login_username_field", "login_error")
        assertDirectlyAbove("login_error", "login_password_field")
    }

    @Test
    fun `an unreachable server is reported above the address`() {
        show(LoginError.Unreachable)

        compose.onNodeWithTag("login_error")
            .assertTextContains(string(R.string.login_error_unreachable))
        assertDirectlyAbove("login_error", "login_host_field")
    }

    @Test
    fun `a missing username is reported above the username field`() {
        show(LoginError.UsernameMissing)

        assertDirectlyAbove("login_host_field", "login_error")
        assertDirectlyAbove("login_error", "login_username_field")
    }

    @Test
    fun `a server error quotes its status code`() {
        show(LoginError.ServerError(502))

        compose.onNodeWithTag("login_error")
            .assertTextContains(string(R.string.login_error_server, 502))
    }

    @Test
    fun `every error has its own wording`() {
        val errors = listOf(
            LoginError.HostMissing, LoginError.HostScheme, LoginError.UsernameMissing,
            LoginError.ApiKeyMissing, LoginError.PasswordRejected, LoginError.ApiKeyRejected,
            LoginError.Unreachable, LoginError.ServerError(500), LoginError.Unexpected,
            LoginError.UnusableSession
        )
        val wordings = errors.map { compose.activity.resources.let { r -> r.getString(it.message, 500) } }

        assertEquals(errors.size, wordings.toSet().size)
    }

    // A screen reader should hear it without having to find it.
    @Test
    fun `the message is announced`() {
        show(LoginError.Unreachable)

        val mode = compose.onNodeWithTag("login_error").fetchSemanticsNode()
            .config[SemanticsProperties.LiveRegion]
        assertEquals(LiveRegionMode.Polite, mode)
    }

    // Switching to an API key hides the password field; a password error must
    // not vanish with it.
    @Test
    fun `an error for a hidden field still shows, at the top of the form`() {
        show(LoginError.PasswordRejected)
        compose.onNodeWithTag("login_mode_toggle")
            .performSemanticsAction(SemanticsActions.RequestFocus)
        compose.onNodeWithTag("login_mode_toggle")
            .performKeyInput { pressKey(Key.DirectionCenter) }
        compose.waitForIdle()

        compose.onNodeWithTag("login_password_field").assertDoesNotExist()
        assertDirectlyAbove("login_error", "login_host_field")
    }
}
