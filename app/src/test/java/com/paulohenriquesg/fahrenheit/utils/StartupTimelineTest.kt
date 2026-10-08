package com.paulohenriquesg.fahrenheit.utils

import org.junit.Assert.assertEquals
import org.junit.Test

/** Where a cold start's time goes, as log lines (#191). */
class StartupTimelineTest {

    private var now = 0L
    private val lines = mutableListOf<String>()
    private val timeline = StartupTimeline(origin = { 1_000L }, now = { now }, log = { lines += it })

    @Test
    fun `each step is logged in ms since the process started`() {
        now = 1_250L
        timeline.mark("login")
        now = 1_900L
        timeline.mark("home", "shelves=3")

        assertEquals(listOf("login at 250 ms", "home at 900 ms (shelves=3)"), lines)
    }

    // Home loads its shelves again on every library switch; only the first is the cold start.
    @Test
    fun `a step is logged only the first time`() {
        now = 1_100L
        timeline.mark("home")
        now = 5_000L
        timeline.mark("home")

        assertEquals(listOf("home at 100 ms"), lines)
    }
}
