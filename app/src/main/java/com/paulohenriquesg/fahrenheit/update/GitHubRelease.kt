package com.paulohenriquesg.fahrenheit.update

import com.google.gson.annotations.SerializedName

data class GitHubRelease(
    @SerializedName("tag_name") val tagName: String,
    @SerializedName("name") val name: String,
    @SerializedName("html_url") val htmlUrl: String,
    @SerializedName("body") val body: String?,
    @SerializedName("assets") val assets: List<ReleaseAsset>,
    @SerializedName("prerelease") val prerelease: Boolean = false,
    @SerializedName("draft") val draft: Boolean = false
)

data class ReleaseAsset(
    @SerializedName("name") val name: String,
    @SerializedName("browser_download_url") val browserDownloadUrl: String
)

data class UpdateInfo(
    val availableVersion: String,
    val currentVersion: String,
    val downloadUrl: String,
    val releaseUrl: String,
    val changelog: String?
)

enum class UpdateStatus {
    UPDATE_AVAILABLE,
    UP_TO_DATE,
    ERROR
}
