package com.paulohenriquesg.fahrenheit.login

import androidx.activity.ComponentActivity
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.dp
import com.paulohenriquesg.fahrenheit.R
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Frame 1 of the login mock (#101): on first run the screen lists the
 * Audiobookshelf servers that answered on this network; typing an address is
 * the fallback, not the default.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w960dp-h540dp")
// Real text metrics: the keyboard check is about heights.
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LoginFindServerTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private var scans = 0

    private fun show(vararg servers: FoundServer, hold: CompletableDeferred<Unit>? = null) {
        compose.setContent {
            FahrenheitTheme {
                LoginScreen(
                    { _, _, _, _ -> },
                    { _, _, _ -> },
                    findServers = { onFound ->
                        scans++
                        servers.forEach(onFound)
                        hold?.await()
                    }
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

    private fun string(id: Int, vararg args: Any) = compose.activity.getString(id, *args)

    @Test
    fun `the servers that answered are listed, with the address as a fallback`() {
        show(FoundServer("http://10.0.0.2:13378", "2.17.0"), FoundServer("http://10.0.0.81:13378", null))

        compose.onNodeWithText("10.0.0.2:13378").assertExists()
        compose.onNodeWithText("10.0.0.81:13378").assertExists()
        compose.onNodeWithTag("login_manual_address").assertExists()
        compose.onNodeWithTag("login_host_field").assertDoesNotExist()
    }

    // Probes answer in any order; a list that reshuffles under the cursor is
    // worse than one in address order.
    @Test
    fun `servers are listed in address order`() {
        show(FoundServer("http://10.0.0.81:13378", null), FoundServer("http://10.0.0.2:13378", null))

        val first = compose.onNodeWithText("10.0.0.2:13378").getUnclippedBoundsInRoot()
        val second = compose.onNodeWithText("10.0.0.81:13378").getUnclippedBoundsInRoot()
        assertTrue(first.bottom <= second.top)
    }

    @Test
    fun `choosing a server goes straight to signing in to it`() {
        show(FoundServer("http://10.0.0.2:13378", "2.17.0"))

        press("login_server_10.0.0.2:13378")

        assertEquals(string(R.string.login_sign_in_to, "10.0.0.2:13378"), text("login_title"))
        compose.onNodeWithTag("login_username_field").assertExists()
    }

    @Test
    fun `entering an address instead opens the address field`() {
        show(FoundServer("http://10.0.0.2:13378", null))

        press("login_manual_address")

        compose.onNodeWithTag("login_host_field").assertExists()
    }

    @Test
    fun `while looking, the screen says so`() {
        show(hold = CompletableDeferred())

        compose.onNodeWithText(string(R.string.login_looking_for_servers)).assertExists()
    }

    @Test
    fun `when nothing answers, the screen says so and offers the address`() {
        show()

        compose.onNodeWithText(string(R.string.login_no_servers_found)).assertExists()
        compose.onNodeWithTag("login_manual_address").assertExists()
    }

    @Test
    fun `a different server goes back to the list and looks again`() {
        show(FoundServer("http://10.0.0.2:13378", null))
        press("login_server_10.0.0.2:13378")

        press("login_different_server")

        compose.onNodeWithTag("login_manual_address").assertExists()
        assertEquals(2, scans)
    }

    @Test
    fun `the list sits above the keyboard`() {
        show(FoundServer("http://10.0.0.2:13378", null))

        val keyboardTop = 540.dp * 0.55f
        for (tag in listOf("login_server_10.0.0.2:13378", "login_manual_address")) {
            val bottom = compose.onNodeWithTag(tag).getUnclippedBoundsInRoot().bottom
            assertTrue("$tag ends at $bottom", bottom <= keyboardTop)
        }
    }

    // Each step change removes what held focus; the new step takes it, or the
    // remote does nothing visible on a TV.
    @Test
    fun `choosing a server puts focus on the username`() {
        show(FoundServer("http://10.0.0.2:13378", null))

        press("login_server_10.0.0.2:13378")

        compose.onNodeWithTag("login_username_field").assertIsFocused()
    }

    @Test
    fun `entering an address instead puts focus on the address`() {
        show()

        press("login_manual_address")

        compose.onNodeWithTag("login_host_field").assertIsFocused()
    }

    @Test
    fun `back on the list after a different server, focus is on the first server`() {
        show(FoundServer("http://10.0.0.2:13378", null))
        press("login_server_10.0.0.2:13378")

        press("login_different_server")

        compose.onNodeWithTag("login_server_10.0.0.2:13378").assertIsFocused()
    }

    @Test
    fun `with nothing found, focus goes to entering an address`() {
        show()
        press("login_manual_address")
        compose.onNodeWithTag("login_host_field").performTextInput("http://10.0.0.2:13378")
        press("login_continue")

        press("login_different_server")

        compose.onNodeWithTag("login_manual_address").assertIsFocused()
    }
}
