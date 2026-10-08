package com.paulohenriquesg.fahrenheit.login

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

/** What a stored session is worth at launch (#191). */
class SessionCheckTest {

    private fun http(code: Int) = HttpException(Response.error<Any>(code, "".toResponseBody()))

    private fun check(probe: suspend () -> Unit) = runBlocking { SessionCheck(probe = probe).run() }

    @Test
    fun `the server answering is a session to go Home with`() {
        assertEquals(SessionCheck.Result.Ready, check {})
    }

    @Test
    fun `no answer is the sign-in form, saying the server could not be reached`() {
        assertEquals(
            SessionCheck.Result.SignIn(LoginError.Unreachable),
            check { throw IOException("connection refused") }
        )
    }

    // The 401 authenticator has already tried the refresh token by then.
    @Test
    fun `a rejected session is the sign-in form with nothing to blame`() {
        assertEquals(SessionCheck.Result.SignIn(null), check { throw http(401) })
        assertEquals(SessionCheck.Result.SignIn(null), check { throw http(403) })
    }

    @Test
    fun `a server failing is the sign-in form with its code`() {
        assertEquals(SessionCheck.Result.SignIn(LoginError.ServerError(502)), check { throw http(502) })
    }

    @Test
    fun `anything else is the sign-in form, unexplained`() {
        assertEquals(
            SessionCheck.Result.SignIn(LoginError.Unexpected),
            check { throw IllegalStateException("bad body") }
        )
    }

    // Back, or the screen being rebuilt: not a verdict on the session.
    @Test
    fun `a cancelled check is not a failure`() {
        assertThrows(CancellationException::class.java) {
            check { throw CancellationException("left") }
        }
    }

    // A server that takes the connection and never answers held the launch
    // screen up for the client's 60 s read timeout, with nothing to press.
    @Test
    fun `a server that never answers is unreachable, after the check's own limit`() {
        val result = runBlocking { SessionCheck(timeoutMs = 50) { awaitCancellation() }.run() }

        assertEquals(SessionCheck.Result.SignIn(LoginError.Unreachable), result)
    }
}
