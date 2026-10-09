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

/**
 * What a stored session is worth at launch (#191). Only the server saying no
 * is a reason to ask for the password again: a TV just woken often has no
 * Wi-Fi for its first seconds.
 */
class SessionCheckTest {

    private fun http(code: Int) = HttpException(Response.error<Any>(code, "".toResponseBody()))

    private fun check(probe: suspend () -> Unit) = runBlocking { SessionCheck(probe = probe).run() }

    @Test
    fun `the server answering is a session to go Home with`() {
        assertEquals(SessionCheck.Result.Ready, check {})
    }

    // The 401 authenticator has already spent the refresh token by then.
    @Test
    fun `only the server rejecting the session is a rejection`() {
        assertEquals(SessionCheck.Result.Rejected, check { throw http(401) })
        assertEquals(SessionCheck.Result.Rejected, check { throw http(403) })
    }

    @Test
    fun `no answer is unreachable, not a rejection`() {
        assertEquals(SessionCheck.Result.Unreachable, check { throw IOException("connection refused") })
    }

    @Test
    fun `a server failing is unreachable, not a rejection`() {
        assertEquals(SessionCheck.Result.Unreachable, check { throw http(502) })
    }

    @Test
    fun `anything else is unreachable, not a rejection`() {
        assertEquals(SessionCheck.Result.Unreachable, check { throw IllegalStateException("bad body") })
    }

    // Back, or the screen being rebuilt: not a verdict on the session.
    @Test
    fun `a cancelled check is not a verdict`() {
        assertThrows(CancellationException::class.java) {
            check { throw CancellationException("left") }
        }
    }

    // A server that takes the connection and never answers held the launch
    // screen up for the client's 60 s read timeout.
    @Test
    fun `a server that never answers is unreachable, after the check's own limit`() {
        val result = runBlocking { SessionCheck(timeoutMs = 50) { awaitCancellation() }.run() }

        assertEquals(SessionCheck.Result.Unreachable, result)
    }
}
