package com.paulohenriquesg.fahrenheit.update

/** Remembers the version the user chose Ignore on. */
interface IgnoreStore {
    fun ignoredVersionCode(): Int?
    fun ignore(versionCode: Int)
}

/** The answer to a check the user asked for, where a failure must say so. */
sealed interface CheckResult {
    data object UpToDate : CheckResult
    data class Available(val update: AvailableUpdate) : CheckResult
    data object Failed : CheckResult
}

/**
 * Decides whether to offer an update, with nothing Android-specific attached.
 *
 * "Ignore" hides that version from the launch check for good; the next release
 * is a separate decision and asks as usual (#229). "No" stores nothing.
 */
class ManifestUpdateChecker(
    private val fetchManifest: suspend () -> String?,
    private val installedVersionCode: () -> Int,
    private val language: () -> String,
    private val ignoreStore: IgnoreStore
) {
    /**
     * The update to offer, or null. Quiet about failures, and passes over an
     * ignored version: this is the check nobody asked for.
     */
    suspend fun check(): AvailableUpdate? {
        val result = fetchAndPlan() as? CheckResult.Available ?: return null
        if (isIgnored(result.update.versionCode)) return null
        return result.update
    }

    /**
     * A check the user asked for: it offers an ignored version too, and a
     * failure says so rather than passing for "nothing new".
     */
    suspend fun checkNow(): CheckResult = fetchAndPlan()

    private suspend fun fetchAndPlan(): CheckResult {
        val body = try {
            fetchManifest()
        } catch (e: Exception) {
            // Offline, DNS, a captive portal.
            null
        } ?: return CheckResult.Failed

        val manifest = UpdateManifest.parse(body)
        if (manifest?.versions == null) return CheckResult.Failed
        val update = UpdatePlanner.plan(manifest, installedVersionCode(), language())
            ?: return CheckResult.UpToDate
        return CheckResult.Available(update)
    }

    fun ignore(update: AvailableUpdate) = ignoreStore.ignore(update.versionCode)

    private fun isIgnored(versionCode: Int): Boolean {
        val ignored = ignoreStore.ignoredVersionCode() ?: return false
        return versionCode <= ignored
    }
}
