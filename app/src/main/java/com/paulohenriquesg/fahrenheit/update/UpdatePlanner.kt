package com.paulohenriquesg.fahrenheit.update

/** A published version worth offering, with everything needed to install it. */
data class AvailableUpdate(
    val versionCode: Int,
    val versionName: String,
    val changelog: List<String>,
    val apkUrl: String,
    val sha256: String,
    val sizeBytes: Long
)

object UpdatePlanner {

    /**
     * What to offer someone running [installedVersionCode], or null when there
     * is nothing newer. The notes cover every version above the installed one.
     */
    fun plan(manifest: UpdateManifest, installedVersionCode: Int, language: String): AvailableUpdate? {
        val apk = manifest.apk ?: return null
        val url = apk.url ?: return null
        val sha256 = apk.sha256 ?: return null

        val newer = manifest.versions.orEmpty()
            .filter { (it.versionCode ?: 0) > installedVersionCode }
            .sortedByDescending { it.versionCode }
        val latest = newer.firstOrNull() ?: return null

        return AvailableUpdate(
            versionCode = latest.versionCode ?: return null,
            versionName = latest.versionName ?: return null,
            changelog = newer.flatMap { notesIn(it, language) },
            apkUrl = url,
            sha256 = sha256,
            sizeBytes = apk.sizeBytes ?: 0L
        )
    }

    private fun notesIn(version: UpdateManifest.ManifestVersion, language: String): List<String> =
        version.changelogs?.get(language)?.takeIf { it.isNotEmpty() }
            ?: version.changelog.orEmpty()
}
