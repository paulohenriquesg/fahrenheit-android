package com.paulohenriquesg.fahrenheit.settings

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.input.key.Key
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import com.paulohenriquesg.fahrenheit.ui.theme.ThemePreference
import com.paulohenriquesg.fahrenheit.update.AvailableUpdate
import com.paulohenriquesg.fahrenheit.update.CheckResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w960dp-h540dp")
class SettingsViewTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun render(
        theme: ThemePreference = ThemePreference.System,
        rowLayout: Boolean = true,
        deviceIsDark: Boolean = false,
        update: UpdateCheck = UpdateCheck.Idle,
        onTheme: (ThemePreference) -> Unit = {},
        onLayout: (Boolean) -> Unit = {},
        onCheck: () -> Unit = {},
        onInstall: (AvailableUpdate) -> Unit = {},
        onSignOut: () -> Unit = {},
        deviceName: String = "AFTMODEL",
        onDeviceName: (String) -> Unit = {}
    ) {
        compose.setContent {
            FahrenheitTheme {
                SettingsView(
                    theme = theme,
                    onTheme = onTheme,
                    deviceIsDark = deviceIsDark,
                    rowLayout = rowLayout,
                    onLayout = onLayout,
                    version = "v0.0.10",
                    update = update,
                    onCheckUpdates = onCheck,
                    onInstall = onInstall,
                    username = "admin",
                    server = "http://books.example:13378",
                    onSignOut = onSignOut,
                    deviceName = deviceName,
                    onDeviceName = onDeviceName
                )
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun `the theme can be set back to following the device`() {
        render()

        listOf("System", "Light", "Dark").forEach {
            compose.onNodeWithTag("theme_$it").assertExists()
        }
    }

    // Fire OS reports night mode off while its own menus are dark, so the row
    // says what the TV reports rather than claiming what it is set to.
    @Test
    fun `the theme row says what the TV reports`() {
        render(deviceIsDark = true)
        compose.onNodeWithText("The TV reports dark mode").assertIsDisplayed()
    }

    @Test
    fun `a TV reporting light is said to report light`() {
        render(deviceIsDark = false)
        compose.onNodeWithText("The TV reports light mode").assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `choosing a theme reports which one`() {
        var chosen: ThemePreference? = null
        render(onTheme = { chosen = it })

        compose.onNodeWithTag("theme_Dark").performSemanticsAction(SemanticsActions.RequestFocus)
        compose.onNodeWithTag("theme_Dark").performKeyInput { pressKey(Key.DirectionCenter) }
        compose.waitForIdle()

        assertEquals(ThemePreference.Dark, chosen)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `the shelf layout is a choice between two pictures`() {
        var chosen: Boolean? = null
        render(rowLayout = true, onLayout = { chosen = it })

        compose.onNodeWithTag("layout_grid").assertExists()
        compose.onNodeWithTag("layout_rows").assertExists()
        compose.onNodeWithTag("layout_grid").performSemanticsAction(SemanticsActions.RequestFocus)
        compose.onNodeWithTag("layout_grid").performKeyInput { pressKey(Key.DirectionCenter) }
        compose.waitForIdle()

        assertEquals(false, chosen)
    }

    @Test
    fun `an update check answers in the row that asked, not in a Toast`() {
        render(update = UpdateCheck.UpToDate)

        compose.onNodeWithText("Up to date", substring = true).assertIsDisplayed()
    }

    @Test
    fun `a check in progress says so`() {
        render(update = UpdateCheck.Checking)

        compose.onNodeWithText("Checking", substring = true).assertIsDisplayed()
    }

    private val waiting = AvailableUpdate(
        versionCode = 11,
        versionName = "v0.0.11",
        changelog = listOf("Adds a shelf of recent episodes", "Fixes a crash"),
        apkUrl = "https://example.invalid/app.apk",
        sha256 = "abc",
        sizeBytes = 18L * 1024 * 1024
    )

    @Test
    fun `a waiting update says what it is`() {
        render(update = UpdateCheck.Available(waiting))

        compose.onNodeWithText("v0.0.11 is ready").assertIsDisplayed()
    }

    @Test
    fun `a waiting update says how big it is and what it brings`() {
        render(update = UpdateCheck.Available(waiting))

        compose.onNodeWithText("18 MB \u00b7 Adds a shelf of recent episodes").assertIsDisplayed()
    }

    @Test
    fun `a waiting update with no notes still says how big it is`() {
        render(update = UpdateCheck.Available(waiting.copy(changelog = emptyList())))

        compose.onNodeWithText("18 MB").assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `a waiting update can be installed from the row`() {
        var installed: AvailableUpdate? = null
        render(update = UpdateCheck.Available(waiting), onInstall = { installed = it })

        compose.onNodeWithTag("install_update").performSemanticsAction(SemanticsActions.RequestFocus)
        compose.onNodeWithTag("install_update").performKeyInput { pressKey(Key.DirectionCenter) }
        compose.waitForIdle()

        assertSame(waiting, installed)
    }

    @Test
    fun `there is nothing to install when nothing is waiting`() {
        render(update = UpdateCheck.UpToDate)

        compose.onNodeWithTag("install_update").assertDoesNotExist()
    }

    @Test
    fun `a failed check says it failed, not that all is well`() {
        render(update = UpdateCheck.Failed)

        compose.onNodeWithText("Couldn't reach the update server", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Up to date", substring = true).assertDoesNotExist()
    }

    @Test
    fun `the row reads each answer of a check the user asked for`() {
        assertEquals(UpdateCheck.UpToDate, UpdateCheck.from(CheckResult.UpToDate))
        assertEquals(UpdateCheck.Failed, UpdateCheck.from(CheckResult.Failed))
        assertEquals(UpdateCheck.Available(waiting), UpdateCheck.from(CheckResult.Available(waiting)))
    }

    @Test
    fun `the account says who and where`() {
        render()

        // The Playback group (#108) moved the account below the first screenful.
        compose.onNodeWithText("admin").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("http://books.example:13378").performScrollTo().assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    private fun press(tag: String) {
        compose.onNodeWithTag(tag).performSemanticsAction(SemanticsActions.RequestFocus)
        compose.onNodeWithTag(tag).performKeyInput { pressKey(Key.DirectionCenter) }
        compose.waitForIdle()
    }

    @Test
    fun `the device name says what this TV is called`() {
        render(deviceName = "Living room TV")

        // Below the fold at 540dp; the D-pad scrolls it in the same way.
        compose.onNodeWithText("Device name").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Living room TV").performScrollTo().assertIsDisplayed()
    }

    // A text field passed on the way down the page would pop the keyboard; the
    // field only appears when asked for.
    @Test
    fun `there is no text field until renaming is asked for`() {
        render()

        compose.onNodeWithTag("device_name_field").assertDoesNotExist()
    }

    @Test
    fun `renaming hands over the new name`() {
        var named: String? = null
        render(onDeviceName = { named = it })

        press("device_name_rename")
        compose.onNodeWithTag("device_name_field").performTextReplacement("Living room TV")
        press("device_name_save")

        assertEquals("Living room TV", named)
        compose.onNodeWithTag("device_name_field").assertDoesNotExist()
    }

    @Test
    fun `the keyboard's done key saves the name too`() {
        var named: String? = null
        render(onDeviceName = { named = it })

        press("device_name_rename")
        compose.onNodeWithTag("device_name_field").performTextReplacement("Bedroom TV")
        compose.onNodeWithTag("device_name_field").performImeAction()
        compose.waitForIdle()

        assertEquals("Bedroom TV", named)
    }

    @Test
    fun `renaming starts from the current name`() {
        render(deviceName = "Kitchen")

        press("device_name_rename")

        compose.onNodeWithTag("device_name_field").assert(
            androidx.compose.ui.test.hasText("Kitchen")
        )
    }

    // On the stick, saving dropped focus on the rail's Home item: the field and
    // Save left the screen while focused, and focus went wherever it could.
    @Test
    fun `after the keyboard saves, focus is back on Rename`() {
        render()

        press("device_name_rename")
        compose.onNodeWithTag("device_name_field").performTextReplacement("Bedroom TV")
        compose.onNodeWithTag("device_name_field").performImeAction()
        compose.waitForIdle()

        compose.onNodeWithTag("device_name_rename").assertIsFocused()
    }

    @Test
    fun `after Save, focus is back on Rename`() {
        render()

        press("device_name_rename")
        press("device_name_save")

        compose.onNodeWithTag("device_name_rename").assertIsFocused()
    }

    // What the app asks the keyboard for, read where the keyboard reads it.
    @Test
    fun `the name field asks the keyboard for Done`() {
        render()
        press("device_name_rename")

        var action = -1
        compose.runOnUiThread {
            val view = compose.activity.findViewById<android.view.ViewGroup>(android.R.id.content).getChildAt(0)
            val editorView = generateSequence(view) { (it as? android.view.ViewGroup)?.getChildAt(0) }
                .first { it.onCheckIsTextEditor() }
            val info = android.view.inputmethod.EditorInfo()
            editorView.onCreateInputConnection(info)
            action = info.imeOptions and android.view.inputmethod.EditorInfo.IME_MASK_ACTION
        }

        assertEquals(android.view.inputmethod.EditorInfo.IME_ACTION_DONE, action)
    }

    // The Fire TV keyboard labels its action key "Next" even when Done is asked
    // for; whatever it sends, the name is saved rather than focus wandering off.
    @Test
    fun `the keyboard's Next key saves the name as well`() {
        var named: String? = null
        render(onDeviceName = { named = it })

        press("device_name_rename")
        compose.onNodeWithTag("device_name_field").performTextReplacement("Hall TV")
        compose.runOnUiThread {
            val view = compose.activity.findViewById<android.view.ViewGroup>(android.R.id.content).getChildAt(0)
            val editorView = generateSequence(view) { (it as? android.view.ViewGroup)?.getChildAt(0) }
                .first { it.onCheckIsTextEditor() }
            editorView.onCreateInputConnection(android.view.inputmethod.EditorInfo())!!
                .performEditorAction(android.view.inputmethod.EditorInfo.IME_ACTION_NEXT)
        }
        compose.waitForIdle()

        assertEquals("Hall TV", named)
    }
}
