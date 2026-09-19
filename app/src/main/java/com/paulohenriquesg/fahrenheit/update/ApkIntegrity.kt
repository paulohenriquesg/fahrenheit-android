package com.paulohenriquesg.fahrenheit.update

import java.io.File
import java.security.MessageDigest

/**
 * Checks a downloaded APK against the hash published in the manifest, so a
 * truncated download or a captive portal's login page can never be handed to
 * the installer.
 */
object ApkIntegrity {

    fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { stream ->
            val buffer = ByteArray(8192)
            while (true) {
                val read = stream.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun matches(file: File, expectedSha256: String): Boolean {
        if (expectedSha256.isBlank()) return false
        return sha256(file).equals(expectedSha256.trim(), ignoreCase = true)
    }
}
