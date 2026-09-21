package com.paulohenriquesg.fahrenheit.api

import okhttp3.logging.HttpLoggingInterceptor
import org.junit.Assert.assertEquals
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
}
