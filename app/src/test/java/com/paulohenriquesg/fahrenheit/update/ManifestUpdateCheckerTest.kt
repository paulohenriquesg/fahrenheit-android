package com.paulohenriquesg.fahrenheit.update

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ManifestUpdateCheckerTest {

    private val manifestJson = """
        { "apk": { "url": "https://example.invalid/app.apk", "sha256": "abc", "sizeBytes": 10 },
          "versions": [ { "versionCode": 12, "versionName": "v0.0.12", "changelog": ["Fixes"] } ] }
    """.trimIndent()

    private class FakeSnooze(var versionCode: Int? = null, var at: Long? = null) : SnoozeStore {
        override fun snoozedVersionCode() = versionCode
        override fun snoozedAt() = at
        override fun snooze(versionCode: Int, at: Long) {
            this.versionCode = versionCode; this.at = at
        }
    }

    private fun checker(
        fetch: suspend () -> String? = { manifestJson },
        installed: Int = 10,
        snooze: SnoozeStore = FakeSnooze(),
        now: Long = 1_000_000L
    ) = ManifestUpdateChecker(fetch, { installed }, { "en" }, snooze) { now }

    @Test
    fun `offers an update when nothing is snoozed`() = runBlocking {
        assertEquals(12, checker().check()?.versionCode)
    }

    @Test
    fun `stays quiet for a day after the user chooses later`() = runBlocking {
        val snooze = FakeSnooze(versionCode = 12, at = 1_000_000L)

        assertNull(checker(snooze = snooze, now = 1_000_000L + 23 * 3600_000L).check())
    }

    @Test
    fun `asks again once the day has passed`() = runBlocking {
        val snooze = FakeSnooze(versionCode = 12, at = 1_000_000L)

        assertEquals(12, checker(snooze = snooze, now = 1_000_000L + 25 * 3600_000L).check()?.versionCode)
    }

    // Snoozing one version must not hide the next one.
    @Test
    fun `a snooze on an older version does not hide a newer one`() = runBlocking {
        val snooze = FakeSnooze(versionCode = 11, at = 1_000_000L)

        assertEquals(12, checker(snooze = snooze, now = 1_000_000L).check()?.versionCode)
    }

    @Test
    fun `a check the user asked for ignores the snooze`() = runBlocking {
        val snooze = FakeSnooze(versionCode = 12, at = 1_000_000L)

        assertEquals(12, checker(snooze = snooze, now = 1_000_000L).check(force = true)?.versionCode)
    }

    @Test
    fun `a failed fetch is quiet, not a crash`() = runBlocking {
        assertNull(checker(fetch = { null }).check())
        assertNull(checker(fetch = { throw java.io.IOException("offline") }).check())
    }

    @Test
    fun `choosing later records the snooze`() = runBlocking {
        val snooze = FakeSnooze()
        val c = checker(snooze = snooze)

        c.snooze(c.check()!!)

        assertEquals(12, snooze.snoozedVersionCode())
        assertEquals(1_000_000L, snooze.snoozedAt())
    }
}
