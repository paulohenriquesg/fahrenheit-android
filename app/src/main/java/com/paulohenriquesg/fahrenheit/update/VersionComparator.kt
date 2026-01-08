package com.paulohenriquesg.fahrenheit.update

object VersionComparator {
    /**
     * Compares two version strings.
     * Strips "v" prefix and compares semantic version numbers.
     *
     * @param currentVersion Current app version (e.g., "v0.0.6")
     * @param latestVersion Latest available version (e.g., "v0.0.7")
     * @return UpdateStatus indicating if update is available
     */
    fun compareVersions(currentVersion: String, latestVersion: String): UpdateStatus {
        try {
            val current = parseVersion(currentVersion)
            val latest = parseVersion(latestVersion)

            // Compare major.minor.patch
            return when {
                latest.major > current.major -> UpdateStatus.UPDATE_AVAILABLE
                latest.major < current.major -> UpdateStatus.UP_TO_DATE

                latest.minor > current.minor -> UpdateStatus.UPDATE_AVAILABLE
                latest.minor < current.minor -> UpdateStatus.UP_TO_DATE

                latest.patch > current.patch -> UpdateStatus.UPDATE_AVAILABLE
                else -> UpdateStatus.UP_TO_DATE
            }
        } catch (e: Exception) {
            // If we can't parse versions, try string comparison
            return if (latestVersion != currentVersion) {
                UpdateStatus.UPDATE_AVAILABLE
            } else {
                UpdateStatus.UP_TO_DATE
            }
        }
    }

    fun parseVersion(version: String): Version {
        // Strip "v" prefix if present
        val cleanVersion = version.removePrefix("v").removePrefix("V")

        // Split by "." and parse
        val parts = cleanVersion.split(".")

        val major = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val patch = parts.getOrNull(2)?.toIntOrNull() ?: 0

        return Version(major, minor, patch)
    }

    data class Version(
        val major: Int,
        val minor: Int,
        val patch: Int
    ) : Comparable<Version> {
        override fun compareTo(other: Version): Int {
            return when {
                this.major != other.major -> this.major.compareTo(other.major)
                this.minor != other.minor -> this.minor.compareTo(other.minor)
                else -> this.patch.compareTo(other.patch)
            }
        }
    }
}
