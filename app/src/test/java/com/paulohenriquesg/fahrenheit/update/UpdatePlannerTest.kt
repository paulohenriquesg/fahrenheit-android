package com.paulohenriquesg.fahrenheit.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * What the app should offer, given a published manifest and what is installed.
 *
 * A TV app is updated rarely, so the notes shown cover every version since the
 * installed one, not just the newest.
 */
class UpdatePlannerTest {

    private val json = """
        {
          "apk": {
            "url": "https://example.invalid/app-release.apk",
            "sha256": "abc123",
            "sizeBytes": 14791733
          },
          "versions": [
            { "versionCode": 12, "versionName": "v0.0.12",
              "changelog": ["Latest episodes list works again"],
              "changelogs": { "pt-BR": ["Lista de episódios recentes funciona de novo"] } },
            { "versionCode": 11, "versionName": "v0.0.11",
              "changelog": ["Sign in without a password", "Stats screen loads"] },
            { "versionCode": 10, "versionName": "v0.0.10",
              "changelog": ["Older release"] }
          ]
        }
    """.trimIndent()

    private fun manifest() = UpdateManifest.parse(json)!!

    @Test
    fun `offers the newest version when an older one is installed`() {
        val update = UpdatePlanner.plan(manifest(), installedVersionCode = 11, language = "en")!!

        assertEquals("v0.0.12", update.versionName)
        assertEquals(12, update.versionCode)
        assertEquals("https://example.invalid/app-release.apk", update.apkUrl)
        assertEquals("abc123", update.sha256)
        assertEquals(14791733L, update.sizeBytes)
    }

    @Test
    fun `offers nothing when the newest version is installed`() =
        assertNull(UpdatePlanner.plan(manifest(), installedVersionCode = 12, language = "en"))

    // A locally built APK can carry a higher code than anything published.
    @Test
    fun `offers nothing when the installed version is newer`() =
        assertNull(UpdatePlanner.plan(manifest(), installedVersionCode = 99, language = "en"))

    @Test
    fun `collects the notes of every version since the installed one`() {
        val update = UpdatePlanner.plan(manifest(), installedVersionCode = 10, language = "en")!!

        assertEquals(
            listOf(
                "Latest episodes list works again",
                "Sign in without a password",
                "Stats screen loads"
            ),
            update.changelog
        )
    }

    @Test
    fun `prefers notes in the device language and falls back per version`() {
        val update = UpdatePlanner.plan(manifest(), installedVersionCode = 10, language = "pt-BR")!!

        assertEquals(
            listOf(
                "Lista de episódios recentes funciona de novo",
                "Sign in without a password",
                "Stats screen loads"
            ),
            update.changelog
        )
    }

    @Test
    fun `a manifest listing no versions offers nothing`() {
        val empty = UpdateManifest.parse("""{"apk":{"url":"u","sha256":"s","sizeBytes":1},"versions":[]}""")!!

        assertNull(UpdatePlanner.plan(empty, installedVersionCode = 1, language = "en"))
    }

    @Test
    fun `rubbish in place of a manifest is not a crash`() {
        assertNull(UpdateManifest.parse("<html>404</html>"))
        assertNull(UpdateManifest.parse(""))
    }
}
