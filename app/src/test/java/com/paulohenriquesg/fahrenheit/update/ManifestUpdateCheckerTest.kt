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

    private class FakeIgnore(var versionCode: Int? = null) : IgnoreStore {
        override fun ignoredVersionCode() = versionCode
        override fun ignore(versionCode: Int) {
            this.versionCode = versionCode
        }
    }

    private fun manifest(versionCode: Int) = manifestJson.replace("12", versionCode.toString())

    private fun checker(
        fetch: suspend () -> String? = { manifestJson },
        installed: Int = 10,
        ignored: IgnoreStore = FakeIgnore()
    ) = ManifestUpdateChecker(fetch, { installed }, { "en" }, ignored)

    @Test
    fun `offers an update when nothing is ignored`() = runBlocking {
        assertEquals(12, checker().check()?.versionCode)
    }

    // No stores nothing, so the next launch asks about the same version.
    @Test
    fun `offers the same version again on every launch until it is ignored`() = runBlocking {
        val ignore = FakeIgnore()

        assertEquals(12, checker(ignored = ignore).check()?.versionCode)
        assertEquals(12, checker(ignored = ignore).check()?.versionCode)
        assertNull(ignore.ignoredVersionCode())
    }

    @Test
    fun `an ignored version is never offered on launch again`() = runBlocking {
        assertNull(checker(ignored = FakeIgnore(versionCode = 12)).check())
    }

    // Ignoring one version must not hide the next one.
    @Test
    fun `a newer release than the ignored one is offered`() = runBlocking {
        assertEquals(13, checker(fetch = { manifest(13) }, ignored = FakeIgnore(versionCode = 12)).check()?.versionCode)
    }

    @Test
    fun `a check the user asked for still offers an ignored version`() = runBlocking {
        val result = checker(ignored = FakeIgnore(versionCode = 12)).checkNow()

        assertEquals(12, (result as CheckResult.Available).update.versionCode)
    }

    @Test
    fun `a check the user asked for says when there is nothing newer`() = runBlocking {
        assertEquals(CheckResult.UpToDate, checker(installed = 12).checkNow())
    }

    // Asked for, a failure must not read as "nothing new": the listener would
    // believe they are up to date while offline.
    @Test
    fun `a check the user asked for reports a failed fetch`() = runBlocking {
        assertEquals(CheckResult.Failed, checker(fetch = { null }).checkNow())
        assertEquals(CheckResult.Failed, checker(fetch = { throw java.io.IOException("offline") }).checkNow())
    }

    // A captive portal answers with its login page, not with a manifest.
    @Test
    fun `a check the user asked for reports an answer that is not a manifest`() = runBlocking {
        assertEquals(CheckResult.Failed, checker(fetch = { "<html><body>Sign in</body></html>" }).checkNow())
        assertEquals(CheckResult.Failed, checker(fetch = { "{}" }).checkNow())
    }

    @Test
    fun `a failed fetch is quiet, not a crash`() = runBlocking {
        assertNull(checker(fetch = { null }).check())
        assertNull(checker(fetch = { throw java.io.IOException("offline") }).check())
    }

    @Test
    fun `ignoring records that version`() = runBlocking {
        val ignore = FakeIgnore()
        val c = checker(ignored = ignore)

        c.ignore(c.check()!!)

        assertEquals(12, ignore.ignoredVersionCode())
        assertNull(c.check())
    }
}
