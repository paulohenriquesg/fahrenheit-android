package com.paulohenriquesg.fahrenheit.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * The app reads a manifest that the release workflow writes in another
 * language, so the two can drift apart unnoticed. The fixture here is the
 * actual output of scripts/build-update-manifest.py.
 */
class PublishedManifestTest {

    private val published: String =
        javaClass.getResourceAsStream("/update-manifest-sample.json")!!.bufferedReader().readText()

    @Test
    fun `a manifest from the release script is understood`() {
        val manifest = UpdateManifest.parse(published)
        assertNotNull(manifest)

        val update = UpdatePlanner.plan(manifest!!, installedVersionCode = 10, language = "en")!!

        assertEquals(12, update.versionCode)
        assertEquals("v0.0.12", update.versionName)
        assertEquals(
            "https://github.com/paulohenriquesg/fahrenheit-android/releases/download/latest/app-release.apk",
            update.apkUrl
        )
        assertEquals(64, update.sha256.length)
        assertEquals(11L, update.sizeBytes)
        assertEquals(
            listOf(
                "Sign in without a password",
                "Latest episodes list works again",
                "Stats screen loads"
            ),
            update.changelog
        )
    }

    @Test
    fun `someone already on the newest version is offered nothing`() {
        val manifest = UpdateManifest.parse(published)!!

        assertEquals(null, UpdatePlanner.plan(manifest, installedVersionCode = 12, language = "en"))
    }
}
