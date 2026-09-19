package com.paulohenriquesg.fahrenheit.update

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

/**
 * The update manifest published next to each release APK.
 *
 * It lists every released version, newest first, so a TV left un-updated for
 * months can show the notes of everything it missed. Only the newest APK is
 * downloadable; older entries exist for their notes alone.
 */
data class UpdateManifest(
    @SerializedName("apk") val apk: ApkFile?,
    @SerializedName("versions") val versions: List<ManifestVersion>?
) {
    data class ApkFile(
        @SerializedName("url") val url: String?,
        @SerializedName("sha256") val sha256: String?,
        @SerializedName("sizeBytes") val sizeBytes: Long?
    )

    data class ManifestVersion(
        @SerializedName("versionCode") val versionCode: Int?,
        @SerializedName("versionName") val versionName: String?,
        /** Release notes in English. */
        @SerializedName("changelog") val changelog: List<String>?,
        /** Notes per language tag, e.g. "pt-BR"; English is the fallback. */
        @SerializedName("changelogs") val changelogs: Map<String, List<String>>?
    )

    companion object {
        /**
         * Reads a manifest, or null if the body is not one. A failing update
         * check must never take the app down, and what comes back from a CDN
         * can be an error page rather than JSON.
         */
        fun parse(json: String): UpdateManifest? = try {
            Gson().fromJson(json, UpdateManifest::class.java)
        } catch (e: Exception) {
            null
        }
    }
}
