package com.paulohenriquesg.fahrenheit.settings

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.input.key.Key
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import com.paulohenriquesg.fahrenheit.ui.theme.ThemePreference
import org.junit.Assert.assertEquals
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
        update: UpdateCheck = UpdateCheck.Idle,
        onTheme: (ThemePreference) -> Unit = {},
        onLayout: (Boolean) -> Unit = {},
        onCheck: () -> Unit = {},
        onSignOut: () -> Unit = {}
    ) {
        compose.setContent {
            FahrenheitTheme {
                SettingsView(
                    theme = theme,
                    onTheme = onTheme,
                    rowLayout = rowLayout,
                    onLayout = onLayout,
                    version = "v0.0.10",
                    update = update,
                    onCheckUpdates = onCheck,
                    username = "admin",
                    server = "http://books.example:13378",
                    onSignOut = onSignOut
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

    @Test
    fun `a waiting update says what it is`() {
        render(update = UpdateCheck.Available("v0.0.11"))

        compose.onNodeWithText("v0.0.11", substring = true).assertIsDisplayed()
    }

    @Test
    fun `the account says who and where`() {
        render()

        compose.onNodeWithText("admin").assertIsDisplayed()
        compose.onNodeWithText("http://books.example:13378").assertIsDisplayed()
    }
}
