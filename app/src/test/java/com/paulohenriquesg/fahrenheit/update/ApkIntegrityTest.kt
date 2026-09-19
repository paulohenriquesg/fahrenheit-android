package com.paulohenriquesg.fahrenheit.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ApkIntegrityTest {

    @get:Rule
    val folder = TemporaryFolder()

    private fun fileOf(text: String) = folder.newFile().apply { writeText(text) }

    @Test
    fun `hashes a file`() {
        // Known SHA-256 of "abc".
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            ApkIntegrity.sha256(fileOf("abc"))
        )
    }

    @Test
    fun `accepts a file matching the published hash`() {
        val file = fileOf("release apk bytes")

        assertTrue(ApkIntegrity.matches(file, ApkIntegrity.sha256(file)))
    }

    @Test
    fun `a published hash in capitals still matches`() {
        val file = fileOf("release apk bytes")

        assertTrue(ApkIntegrity.matches(file, ApkIntegrity.sha256(file).uppercase()))
    }

    // A proxy or captive portal serving something else must not reach install.
    @Test
    fun `rejects a file that is not what was published`() {
        assertFalse(ApkIntegrity.matches(fileOf("<html>login</html>"), ApkIntegrity.sha256(fileOf("real"))))
    }

    @Test
    fun `rejects when the manifest carries no hash`() {
        assertFalse(ApkIntegrity.matches(fileOf("anything"), ""))
    }
}
