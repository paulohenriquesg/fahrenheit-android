package com.paulohenriquesg.fahrenheit.login

import android.os.Looper
import androidx.compose.runtime.mutableStateOf
import androidx.test.core.app.ApplicationProvider
import com.paulohenriquesg.fahrenheit.api.FakeTokenStore
import com.paulohenriquesg.fahrenheit.auth.AuthSession
import com.paulohenriquesg.fahrenheit.auth.SessionManager
import com.paulohenriquesg.fahrenheit.auth.SessionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowToast
import java.io.IOException

/**
 * A failed sign-in lands in state the screen draws beside the field, not in a
 * Toast under the keyboard (#99).
 */
@RunWith(RobolectricTestRunner::class)
class LoginHandlerTest {

    private fun handler(login: suspend (String, String, String) -> AuthSession) = LoginHandler(
        ApplicationProvider.getApplicationContext(),
        LoginCoordinator(
            sessionManager = SessionManager(FakeTokenStore()),
            performLogin = login,
            performApiKeyLogin = { _, _ -> error("not used") },
            activateSession = { SessionState.Ready }
        )
    )

    private fun settle() = shadowOf(Looper.getMainLooper()).idle()

    @Test
    fun `a failure becomes the screen's error, and no Toast is shown`() {
        val h = handler { _, _, _ -> throw IOException("connection refused") }

        h.handleLogin("http://abs.local", "someone", "hunter2", mutableStateOf(false))
        settle()

        assertEquals(LoginError.Unreachable, h.error.value)
        assertNull(ShadowToast.getLatestToast())
    }

    @Test
    fun `trying again clears the last error`() {
        var fail = true
        val h = handler { _, _, _ ->
            if (fail) throw IOException("down") else AuthSession("a", "r", "someone")
        }
        h.handleLogin("http://abs.local", "someone", "hunter2", mutableStateOf(false))
        settle()

        fail = false
        h.handleLogin("http://abs.local", "", "hunter2", mutableStateOf(false))
        settle()

        assertEquals(LoginError.UsernameMissing, h.error.value)
    }

    @Test
    fun `loading ends when the attempt does`() {
        val loading = mutableStateOf(false)
        val h = handler { _, _, _ -> throw IOException("down") }

        h.handleLogin("http://abs.local", "someone", "hunter2", loading)
        settle()

        assertEquals(false, loading.value)
    }
}
