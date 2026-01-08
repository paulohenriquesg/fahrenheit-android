package com.paulohenriquesg.fahrenheit.update

import android.content.Context
import com.paulohenriquesg.fahrenheit.storage.SharedPreferencesHandler
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

object UpdateChecker {
    private const val CHECK_INTERVAL_MS = 24 * 60 * 60 * 1000L // 24 hours in milliseconds

    // Test mode for development
    var TEST_MODE = false
    var TEST_RESPONSE: GitHubRelease? = null

    /**
     * Check for app updates from GitHub releases
     * @param currentVersion Current app version (e.g., "v0.0.6")
     * @param context Application context for preferences
     * @param callback Called with UpdateInfo if update available, null otherwise
     */
    fun checkForUpdate(
        currentVersion: String,
        context: Context,
        callback: (UpdateInfo?) -> Unit
    ) {
        // Test mode for development
        if (TEST_MODE) {
            handleTestMode(currentVersion, callback)
            return
        }

        // Make API call
        val apiService = GitHubApiClient.getApiService()
        apiService.getReleases(
            GitHubApiClient.getOwner(),
            GitHubApiClient.getRepo()
        ).enqueue(object : Callback<List<GitHubRelease>> {
            override fun onResponse(call: Call<List<GitHubRelease>>, response: Response<List<GitHubRelease>>) {
                if (response.isSuccessful) {
                    val releases = response.body() ?: emptyList()

                    // Filter for stable semantic versions only
                    val stableReleases = releases
                        .filter { !it.prerelease && !it.draft }
                        .filter { it.tagName.matches(Regex("^v?\\d+\\.\\d+\\.\\d+$")) }
                        .sortedByDescending { VersionComparator.parseVersion(it.tagName) }

                    val latestRelease = stableReleases.firstOrNull()
                    if (latestRelease != null) {
                        handleRelease(latestRelease, currentVersion, context, callback)
                    } else {
                        callback(null)
                    }
                } else {
                    callback(null)
                }
            }

            override fun onFailure(call: Call<List<GitHubRelease>>, t: Throwable) {
                // Fail silently - don't bother user with network errors
                callback(null)
            }
        })
    }

    private fun handleRelease(
        release: GitHubRelease,
        currentVersion: String,
        context: Context,
        callback: (UpdateInfo?) -> Unit
    ) {
        // Update last check time
        updateLastCheckTime(context)

        // Compare versions
        val status = VersionComparator.compareVersions(currentVersion, release.tagName)

        if (status == UpdateStatus.UPDATE_AVAILABLE) {
            // Check if user skipped this version
            val preferences = SharedPreferencesHandler(context).getUserPreferences()
            if (preferences.skipVersion == release.tagName) {
                callback(null)
                return
            }

            // Find APK download URL
            val apkAsset = release.assets.find { it.name.endsWith(".apk") }
            val downloadUrl = apkAsset?.browserDownloadUrl ?: release.htmlUrl

            val updateInfo = UpdateInfo(
                availableVersion = release.tagName,
                currentVersion = currentVersion,
                downloadUrl = downloadUrl,
                releaseUrl = release.htmlUrl,
                changelog = release.body
            )

            callback(updateInfo)
        } else {
            callback(null)
        }
    }

    private fun handleTestMode(currentVersion: String, callback: (UpdateInfo?) -> Unit) {
        val release = TEST_RESPONSE
        if (release != null) {
            val updateInfo = UpdateInfo(
                availableVersion = release.tagName,
                currentVersion = currentVersion,
                downloadUrl = release.htmlUrl,
                releaseUrl = release.htmlUrl,
                changelog = release.body
            )
            callback(updateInfo)
        } else {
            callback(null)
        }
    }

    /**
     * Check if we should check for updates on app startup
     * @param context Application context
     * @return true if we should check (24h passed and user hasn't disabled)
     */
    fun shouldCheckOnStartup(context: Context): Boolean {
        val preferences = SharedPreferencesHandler(context).getUserPreferences()

        // Check if updates are enabled
        if (!preferences.updateCheckEnabled) {
            return false
        }

        // Check if 24 hours have passed since last check
        val currentTime = System.currentTimeMillis()
        val timeSinceLastCheck = currentTime - preferences.lastUpdateCheck

        return timeSinceLastCheck >= CHECK_INTERVAL_MS
    }

    /**
     * Mark a version as skipped so user won't be prompted again
     * @param context Application context
     * @param version Version to skip (e.g., "v0.0.7")
     */
    fun markVersionSkipped(context: Context, version: String) {
        val handler = SharedPreferencesHandler(context)
        val currentPrefs = handler.getUserPreferences()

        val updatedPrefs = currentPrefs.copy(skipVersion = version)
        handler.saveUserPreferences(updatedPrefs)
    }

    /**
     * Update the last check timestamp
     * @param context Application context
     */
    private fun updateLastCheckTime(context: Context) {
        val handler = SharedPreferencesHandler(context)
        val currentPrefs = handler.getUserPreferences()

        val updatedPrefs = currentPrefs.copy(lastUpdateCheck = System.currentTimeMillis())
        handler.saveUserPreferences(updatedPrefs)
    }
}
