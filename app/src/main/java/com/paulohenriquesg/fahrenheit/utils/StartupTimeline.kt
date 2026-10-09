package com.paulohenriquesg.fahrenheit.utils

import android.os.Process
import android.os.SystemClock
import android.util.Log

/**
 * Where a cold start's time goes (#191): one log line per step, in ms since
 * the process started. `adb logcat -s Startup` reads it off a device.
 *
 * Each step is logged once per process, so Home loading its shelves again
 * for another library is not mistaken for the cold start.
 */
class StartupTimeline(
    private val origin: () -> Long,
    private val now: () -> Long,
    private val log: (String) -> Unit
) {
    private val logged = mutableSetOf<String>()

    fun mark(step: String, detail: String? = null) {
        synchronized(logged) { if (!logged.add(step)) return }
        log("$step at ${now() - origin()} ms" + detail?.let { " ($it)" }.orEmpty())
    }

    companion object {
        /** The app's own, on the elapsedRealtime clock the process start is measured on. */
        val app = StartupTimeline(
            origin = Process::getStartElapsedRealtime,
            now = SystemClock::elapsedRealtime,
            log = { Log.i("Startup", it) }
        )
    }
}
