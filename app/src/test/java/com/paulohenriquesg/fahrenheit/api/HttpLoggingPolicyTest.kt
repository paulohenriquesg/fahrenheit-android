package com.paulohenriquesg.fahrenheit.api

import okhttp3.logging.HttpLoggingInterceptor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HttpLoggingPolicyTest {

    // Nothing secret is printed at BASIC, but a shipped app should not be
    // narrating every request a listener makes into the system log.
    @Test
    fun `a released app logs nothing`() =
        assertEquals(HttpLoggingInterceptor.Level.NONE, HttpLoggingPolicy.level(debugBuild = false))

    // Full bodies OOM on large library responses, so BASIC even while developing.
    @Test
    fun `a debug build logs request lines only`() =
        assertEquals(HttpLoggingInterceptor.Level.BASIC, HttpLoggingPolicy.level(debugBuild = true))

    // #215: the download queue's reply was seen empty mid-download; its body,
    // small and with nothing about what is listened to, says what the server sent.
    @Test
    fun `a debug build logs the download queue's reply in full, and nothing else's`() {
        assertTrue(HttpLoggingPolicy.logsBody("/api/libraries/lib_1/episode-downloads", debugBuild = true))
        assertFalse(HttpLoggingPolicy.logsBody("/api/libraries/lib_1/items", debugBuild = true))
        assertFalse(HttpLoggingPolicy.logsBody("/api/libraries/lib_1/episode-downloads", debugBuild = false))
    }
}
