package com.paulohenriquesg.fahrenheit.login

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.input.key.Key
import com.paulohenriquesg.fahrenheit.R
import com.paulohenriquesg.fahrenheit.storage.SharedPreferencesHandler
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import kotlinx.coroutines.awaitCancellation
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The login screens in the app's look (#161, docs/mocks/login-style.html):
 * a left column over a backdrop, the account as a person, and buttons that
 * show focus without growing. What they ask and do is pinned elsewhere.
 */
@OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w960dp-h540dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LoginStyleTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private var shown by mutableStateOf(true)
    private val asked = mutableListOf<String>()

    // The wall drifts for as long as it is drawn; take it away, or the next
    // test class never sees the clock idle.
    @After
    fun takeTheScreenAway() {
        compose.runOnUiThread { shown = false }
        compose.mainClock.advanceTimeByFrame()
    }

    private fun remember(host: String, username: String) {
        val prefs = SharedPreferencesHandler(compose.activity)
        prefs.saveUserPreferences(prefs.getUserPreferences().copy(host = host, username = username))
    }

    private fun cover(): ImageBitmap =
        Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888).apply { eraseColor(android.graphics.Color.RED) }.asImageBitmap()

    private fun show(
        covers: List<ImageBitmap>? = null,
        findServers: (suspend ((FoundServer) -> Unit) -> Unit)? = null
    ) {
        // The wall and a focused field's cursor never stop moving: the clock
        // is driven by hand.
        compose.mainClock.autoAdvance = false
        compose.setContent {
            FahrenheitTheme {
                if (shown) LoginScreen(
                    { _, _, _, _ -> },
                    { _, _, _ -> },
                    findServers = findServers,
                    loadCovers = covers?.let { list -> { host -> asked += host; list } }
                )
            }
        }
        settle()
    }

    private fun settle() {
        compose.mainClock.advanceTimeBy(1_000)
        compose.runOnIdle { }
    }

    // A TV button answers the centre key, not a tap.
    private fun press(tag: String) {
        compose.onNodeWithTag(tag).performSemanticsAction(SemanticsActions.RequestFocus)
        compose.onNodeWithTag(tag).performKeyInput { pressKey(Key.DirectionCenter) }
        settle()
    }

    private fun exists(tag: String) =
        compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()

    private fun text(tag: String) = compose.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode()
        .config[SemanticsProperties.Text].joinToString(" ") { it.text }

    // --- The backdrop ---------------------------------------------------

    @Test
    fun `welcome back with covers on the device draws them behind the form`() {
        remember("http://abs.local:13378", "someone")

        show(covers = listOf(cover(), cover()))

        assertTrue(exists("login_backdrop_covers"))
        assertTrue(!exists("login_backdrop_field"))
        assertEquals(listOf("http://abs.local:13378"), asked)
    }

    @Test
    fun `welcome back with no covers on the device falls back to the colour field`() {
        remember("http://abs.local:13378", "someone")

        show(covers = emptyList())

        assertTrue(exists("login_backdrop_field"))
        assertTrue(!exists("login_backdrop_covers"))
    }

    @Test
    fun `welcome back with nothing to load covers draws the colour field`() {
        remember("http://abs.local:13378", "someone")

        show(covers = null)

        assertTrue(exists("login_backdrop_field"))
    }

    // Nothing is known on a first run: no server to have covers from.
    @Test
    fun `a first run draws the colour field and asks for no covers`() {
        show(covers = listOf(cover()))

        assertTrue(exists("login_backdrop_field"))
        assertTrue(!exists("login_backdrop_covers"))
        assertEquals(emptyList<String>(), asked)
    }

    @Test
    fun `a different server leaves the covers of the old one behind`() {
        remember("http://abs.local:13378", "someone")
        show(covers = listOf(cover()))

        press("login_different_server")

        assertTrue(exists("login_backdrop_field"))
        assertTrue(!exists("login_backdrop_covers"))
    }

    // --- The column -----------------------------------------------------

    @Test
    fun `every step names the app above its title`() {
        // In capitals, as the mock sets it: a mark, not a sentence.
        val app = compose.activity.getString(R.string.app_name).uppercase(java.util.Locale.ROOT)

        show(findServers = { awaitCancellation() })
        assertEquals(app, text("login_brand"))

        press("login_manual_address")
        assertEquals(app, text("login_brand"))

        compose.onNodeWithTag("login_host_field").performTextInput("http://abs.local:13378")
        press("login_continue")
        assertEquals(app, text("login_brand"))
        val brand = compose.onNodeWithTag("login_brand").fetchSemanticsNode().boundsInRoot
        val title = compose.onNodeWithTag("login_title").fetchSemanticsNode().boundsInRoot
        assertTrue("the app's name ($brand) sits above the title ($title)", brand.bottom <= title.top)
    }

    // --- The account ----------------------------------------------------

    @Test
    fun `the account shows the username's initial in its avatar`() {
        remember("http://abs.local:13378", "someone")

        show()

        assertEquals("S", initial())
    }

    // The name beside it already says who: a reader hears the name, not "S".
    @Test
    fun `the avatar's initial is not read out`() {
        remember("http://abs.local:13378", "someone")

        show()

        val read = compose.onNodeWithTag("login_account").fetchSemanticsNode()
            .config.getOrElse(SemanticsProperties.Text) { emptyList() }.map { it.text }
        assertTrue("read: $read", "S" !in read)
        assertTrue("read: $read", "someone" in read)
    }

    @Test
    fun `an initial outside ASCII is uppercased whole`() {
        remember("http://abs.local:13378", "élodie")

        show()

        assertEquals("É", initial())
    }

    private fun initial() = compose.onNodeWithTag("login_account_avatar", useUnmergedTree = true)
        .fetchSemanticsNode().config[AvatarInitial]

    // --- Focus does not move things (style guide) -----------------------

    // A TV button scales itself on focus; the label inside it is measured,
    // since the scale is drawn inside the node that carries the tag.
    private fun labelWidth(label: String) =
        compose.onNodeWithText(label, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.width

    private fun assertNoGrowth(tag: String, label: String, restOn: String) {
        compose.onNodeWithTag(restOn).performSemanticsAction(SemanticsActions.RequestFocus)
        settle()
        val resting = labelWidth(label)
        compose.onNodeWithTag(tag).performSemanticsAction(SemanticsActions.RequestFocus)
        settle()
        val focused = labelWidth(label)
        assertEquals("$tag grew on focus", resting, focused, 0.5f)
    }

    @Test
    fun `no welcome-back button grows when focused`() {
        remember("http://abs.local:13378", "someone")
        show()
        val activity = compose.activity

        assertNoGrowth("login_submit_button", activity.getString(R.string.login_sign_in), restOn = "login_password_field")
        assertNoGrowth("login_mode_toggle", activity.getString(R.string.login_use_api_key), restOn = "login_password_field")
        assertNoGrowth("login_different_server", activity.getString(R.string.login_different_server), restOn = "login_password_field")
    }

    @Test
    fun `no server row grows when focused`() {
        show(findServers = { onFound -> onFound(FoundServer("http://abs.local:13378", "2.0.0")); awaitCancellation() })

        assertNoGrowth(
            "login_server_abs.local:13378", "abs.local:13378", restOn = "login_manual_address"
        )
    }
}
