package com.paulohenriquesg.fahrenheit.progress

import com.paulohenriquesg.fahrenheit.api.ApiClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

/** The next account to sign in sees none of the last one's progress (#207). */
class SignOutForgetsProgressTest {

    @After
    fun tearDown() = ProgressStore.process.clear()

    @Test
    fun `signing out empties the shared store`() {
        ProgressStore.process.played("book", null, position = 10.0, duration = 100.0)

        ApiClient.clearSession()

        assertEquals(emptySet<ProgressKey>(), ProgressStore.process.entries.value.keys)
    }
}
