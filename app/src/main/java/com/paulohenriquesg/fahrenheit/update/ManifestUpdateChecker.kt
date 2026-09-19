package com.paulohenriquesg.fahrenheit.update

/** Remembers the version the user pushed away, and when. */
interface SnoozeStore {
    fun snoozedVersionCode(): Int?
    fun snoozedAt(): Long?
    fun snooze(versionCode: Int, at: Long)
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
     * The update to offer, or null. [force] is a check the user asked for, so
     * it answers even while a snooze is running.
     */
    suspend fun check(force: Boolean = false): AvailableUpdate? {
        val body = try {
            fetchManifest()
        } catch (e: Exception) {
            // Offline, DNS, a captive portal: never worth interrupting anyone.
            null
        } ?: return null

        val manifest = UpdateManifest.parse(body) ?: return null
        val update = UpdatePlanner.plan(manifest, installedVersionCode(), language()) ?: return null

        if (!force && isSnoozed(update.versionCode)) return null
        return update
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
