package com.paulohenriquesg.fahrenheit.login

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A 960dp-wide screen is the whole TV. A text field stretched across all of it
 * is a metre of empty box on the wall behind an eight-character username.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w960dp-h540dp")
class LoginFormWidthTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `the form does not stretch across the whole screen`() {
        compose.setContent {
            FahrenheitTheme {
                LoginScreen(
                    handleLogin = { _, _, _, _ -> },
                    handleApiKeyLogin = { _, _, _ -> }
                )
            }
        }
        compose.waitForIdle()

        val screen = compose.onNodeWithTag("login_screen").fetchSemanticsNode().size.width
        val field = compose.onNodeWithTag("login_host_field").fetchSemanticsNode().size.width
        with(compose.density) {
            assertTrue(
                "field is ${field.toDp()} of a ${screen.toDp()} screen",
                field.toDp() <= 720.dp
            )
        }
        assertTrue("field ($field) should be narrower than the screen ($screen)", field < screen)
    }
}
