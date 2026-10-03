package com.paulohenriquesg.fahrenheit.main

import androidx.activity.ComponentActivity
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.paulohenriquesg.fahrenheit.navigation.MenuAction
import com.paulohenriquesg.fahrenheit.navigation.MenuItem
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Back on the main screen, with the real rail (#125): from a shelf card to
 * the rail's selected section, from a section's rail to Home, and from Home's
 * rail out of the app.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w960dp-h540dp")
class MainBackTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val items = listOf(
        MenuItem(MainView.HOME.menuItemId, "Home", Icons.Filled.Home, MenuAction.HOME),
        MenuItem(MainView.STATS.menuItemId, "Stats", Icons.Filled.Star, MenuAction.STATS)
    )

    private val card = FocusRequester()
    private var view by mutableStateOf(MainView.HOME)

    private fun render(start: MainView, items: List<MenuItem> = this.items) {
        view = start
        compose.setContent {
            FahrenheitTheme {
                val rail = remember { FocusRequester() }
                var railHasFocus by remember { mutableStateOf(false) }
                MainBackHandler(
                    view = view,
                    railHasFocus = railHasFocus,
                    rail = rail,
                    onGoHome = { view = MainView.HOME }
                )
                NavigationRail(
                    items = items,
                    selectedId = view.menuItemId,
                    onSelect = {},
                    firstFocus = rail,
                    onRailFocusChanged = { railHasFocus = it }
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        BasicText(
                            text = "a book",
                            modifier = Modifier
                                .testTag("shelf_card")
                                .focusRequester(card)
                                .focusable()
                        )
                    }
                }
            }
        }
        compose.runOnUiThread { card.requestFocus() }
        compose.waitForIdle()
    }

    private fun back() {
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
    }

    @Test
    fun `Back from a shelf card on Home lands on Home in the rail`() {
        render(MainView.HOME)

        back()

        compose.onNodeWithTag(menuItemTestTag(MainView.HOME.menuItemId)).assertIsFocused()
        assertFalse(compose.activity.isFinishing)
    }

    @Test
    fun `a second Back on Home is left to the system, which leaves the app`() {
        render(MainView.HOME)

        back()
        assertFalse(compose.activity.onBackPressedDispatcher.hasEnabledCallbacks())
        back()

        assertTrue(compose.activity.isFinishing)
    }

    @Test
    fun `on another section Back goes to its rail item, then Home, and stays in the app`() {
        render(MainView.STATS)

        back()
        compose.onNodeWithTag(menuItemTestTag(MainView.STATS.menuItemId)).assertIsFocused()
        assertEquals(MainView.STATS, view)

        back()
        assertEquals(MainView.HOME, view)
        assertFalse(compose.activity.isFinishing)
    }

    // The rail's requester sits on the selected section; with none on screen
    // it is not attached, and requestFocus throws - that has crashed the app before.
    @Test
    fun `Back with no section in the rail to land on does not crash`() {
        render(MainView.STATS, items = items.take(1))

        back()

        assertFalse(compose.activity.isFinishing)
    }
}
