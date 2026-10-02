package com.paulohenriquesg.fahrenheit.login

import org.junit.Assert.assertEquals
import org.junit.Test

class StartDestinationTest {

    @Test
    fun `a stored session opens the app`() {
        assertEquals(
            StartDestination.Main,
            StartDestination.of(host = "http://books.example:13378", token = "a-token")
        )
    }

    @Test
    fun `a remembered address with no session opens the login form`() {
        // What signing out leaves behind now. Sending this to main made main
        // send it back, forever.
        assertEquals(
            StartDestination.Login,
            StartDestination.of(host = "http://books.example:13378", token = "")
        )
    }

    @Test
    fun `a session with nowhere to send it opens the login form`() {
        assertEquals(StartDestination.Login, StartDestination.of(host = "", token = "a-token"))
    }

    @Test
    fun `a fresh install opens the login form`() {
        assertEquals(StartDestination.Login, StartDestination.of(host = "", token = ""))
    }
}
