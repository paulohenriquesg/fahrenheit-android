package com.paulohenriquesg.fahrenheit.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class InitialFocusTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Composable
    private fun Rows(enabled: Boolean, rowCount: Int) {
        val initialFocus = rememberInitialFocus(enabled, rowCount)
        Box {
            LazyColumn {
                items(rowCount) { index ->
                    BasicText(
                        text = "row $index",
                        modifier = Modifier
                            .testTag("row$index")
                            .let { if (index == 0) it.focusRequester(initialFocus) else it }
                            .focusable()
                    )
                }
            }
        }
    }

    @Test
    fun `focus lands on the first item so a d-pad press acts on it`() {
        compose.setContent { Rows(enabled = true, rowCount = 3) }

        compose.waitUntil { compose.onAllNodes(isFocused()).fetchSemanticsNodes().isNotEmpty() }

        compose.onNodeWithTag("row0").assertIsFocused()
    }

    @Test
    fun `focus is left alone while the drawer is open`() {
        compose.setContent { Rows(enabled = false, rowCount = 3) }

        compose.waitForIdle()

        compose.onNodeWithTag("row0").assertIsNotFocused()
    }

    @Test
    fun `the request keeps trying until the content exists`() {
        compose.setContent {
            // The server's answer, and so the first item, lands after the screen does.
            var loaded by remember { mutableStateOf(false) }
            LaunchedEffectOnce { loaded = true }
            Rows(enabled = true, rowCount = if (loaded) 3 else 0)
        }

        compose.mainClock.advanceTimeBy(1_000)
        compose.waitForIdle()

        compose.onNodeWithTag("row0").assertIsFocused()
    }

    @Test
    fun `asking a target that never appears for focus gives up instead of crashing`() {
        // The drawer used to open, wait 100ms and ask: on a slow frame the items
        // were not attached yet and the exception killed the app.
        var outcome: Boolean? = null
        compose.setContent {
            val orphan = remember { FocusRequester() }
            androidx.compose.runtime.LaunchedEffect(Unit) {
                outcome = orphan.requestFocusWhenAttached()
            }
        }

        compose.mainClock.advanceTimeBy(2_000)
        compose.waitForIdle()

        assertEquals(false, outcome)
    }

    @Test
    fun `it takes focus as soon as the target is attached`() {
        var outcome: Boolean? = null
        compose.setContent {
            val requester = remember { FocusRequester() }
            var attached by remember { mutableStateOf(false) }
            LaunchedEffectOnce { attached = true }
            androidx.compose.runtime.LaunchedEffect(Unit) {
                outcome = requester.requestFocusWhenAttached()
            }
            if (attached) {
                BasicText(
                    text = "late",
                    modifier = Modifier
                        .testTag("late")
                        .focusRequester(requester)
                        .focusable()
                )
            }
        }

        compose.waitUntil { compose.onAllNodes(isFocused()).fetchSemanticsNodes().isNotEmpty() }

        compose.onNodeWithTag("late").assertIsFocused()
        assertEquals(true, outcome)
    }

    @Composable
    private fun LaunchedEffectOnce(block: suspend () -> Unit) {
        androidx.compose.runtime.LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(300)
            block()
        }
    }
}
