package com.paulohenriquesg.fahrenheit.update

/** Remembers the version the user pushed away, and when. */
interface SnoozeStore {
    fun snoozedVersionCode(): Int?
    fun snoozedAt(): Long?
    fun snooze(versionCode: Int, at: Long)
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
 * "Later" hides one version for a day rather than for good: a version skipped
 * forever also skips every fix in it, and the next release is a separate
 * decision.
 */
class ManifestUpdateChecker(
    private val fetchManifest: suspend () -> String?,
    private val installedVersionCode: () -> Int,
    private val language: () -> String,
    private val snoozeStore: SnoozeStore,
    private val now: () -> Long
) {
    /**
     * The update to offer, or null. Quiet about failures, and respects a
     * snooze: this is the check nobody asked for.
     */
    suspend fun check(): AvailableUpdate? {
        val result = fetchAndPlan() as? CheckResult.Available ?: return null
        if (isSnoozed(result.update.versionCode)) return null
        return result.update
    }

    /**
     * A check the user asked for: it answers even while a snooze is running,
     * and a failure says so rather than passing for "nothing new".
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

    fun snooze(update: AvailableUpdate) = snoozeStore.snooze(update.versionCode, now())

    private fun isSnoozed(versionCode: Int): Boolean {
        if (snoozeStore.snoozedVersionCode() != versionCode) return false
        val at = snoozeStore.snoozedAt() ?: return false
        return now() - at < SNOOZE_MS
    }

    private companion object {
        const val SNOOZE_MS = 24 * 60 * 60 * 1000L
    }
}
