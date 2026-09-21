package com.paulohenriquesg.fahrenheit.update

import android.content.Context
import android.content.pm.PackageInfo
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.io.File

/**
 * What the updater will and will not hand to the system installer.
 *
 * This is the last check before an APK is installed over the running app, and
 * it had no tests: a wrong answer here installs whatever a proxy, a captive
 * portal or a half-finished download left on disk.
 */
@RunWith(RobolectricTestRunner::class)
class UpdateServiceTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        UpdateService.resetState()
    }

    @After
    fun tearDown() {
        server.shutdown()
        UpdateService.resetState()
        File(context.cacheDir, "fahrenheit_update.apk").delete()
    }

    /** A body big enough to pass the size floor, and its hash. */
    private fun apkBody(size: Int = 1_500_000, fill: Byte = 7): Pair<Buffer, String> {
        val bytes = ByteArray(size) { fill }
        val file = File.createTempFile("apk", null).apply { writeBytes(bytes) }
        val sha = ApkIntegrity.sha256(file)
        file.delete()
        return Buffer().write(bytes) to sha
    }

    private fun download(sha: String) = runBlocking {
        UpdateService.downloadApk(context, server.url("/app-release.apk").toString(), sha)
    }

    @Test
    fun `a download that does not match the published hash is refused and deleted`() {
        val (body, _) = apkBody()
        server.enqueue(MockResponse().setBody(body))
        // Everything else about the file is right, so the hash is the only
        // thing left to refuse it: otherwise this passes for another reason.
        apkAtCacheParsesAs(context.packageName)

        val result = download("0".repeat(64))

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("checksum"))
        assertTrue(UpdateService.downloadState.value is DownloadState.Error)
        assertFalse("the rejected file must not be left on disk",
            File(context.cacheDir, "fahrenheit_update.apk").exists())
    }

    // A captive portal answers 200 with a login page, which is small.
    @Test
    fun `a body too small to be an apk is refused`() {
        server.enqueue(MockResponse().setBody("<html>sign in to the wifi</html>"))

        // The hash is irrelevant: the size check refuses it first.
        val result = download("0".repeat(64))

        assertTrue(result.isFailure)
    }

    @Test
    fun `a server error is reported, not retried into the installer`() {
        server.enqueue(MockResponse().setResponseCode(503))

        val result = download("0".repeat(64))

        assertTrue(result.isFailure)
        assertTrue(UpdateService.downloadState.value is DownloadState.Error)
    }

    @Test
    fun `an unreachable server is reported rather than thrown`() {
        server.shutdown()

        val result = download("0".repeat(64))

        assertTrue(result.isFailure)
    }

    @Test
    fun `resetting puts the updater back to idle`() {
        server.enqueue(MockResponse().setResponseCode(500))
        download("0".repeat(64))

        UpdateService.resetState()

        assertEquals(DownloadState.Idle, UpdateService.downloadState.value)
    }

    /** Robolectric cannot parse a fake APK, so it is told what one contains. */
    private fun apkAtCacheParsesAs(packageName: String) {
        val path = File(context.cacheDir, "fahrenheit_update.apk").absolutePath
        shadowOf(context.packageManager).setPackageArchiveInfo(
            path,
            PackageInfo().apply { this.packageName = packageName }
        )
    }

    @Test
    fun `a download matching the published hash is accepted`() {
        val (body, sha) = apkBody()
        server.enqueue(MockResponse().setBody(body))
        apkAtCacheParsesAs(context.packageName)

        val result = download(sha)

        assertTrue(result.getOrNull().orEmpty().endsWith("fahrenheit_update.apk"))
        assertTrue(UpdateService.downloadState.value is DownloadState.Complete)
    }

    // The hash would have to match too, but an apk for another app must never
    // reach the installer: it would prompt to install something else entirely.
    @Test
    fun `an apk belonging to another app is refused`() {
        val (body, sha) = apkBody()
        server.enqueue(MockResponse().setBody(body))
        apkAtCacheParsesAs("com.example.something.else")

        val result = download(sha)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("Package name mismatch"))
    }
}
