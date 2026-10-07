package com.paulohenriquesg.fahrenheit.podcast

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.paulohenriquesg.fahrenheit.api.LibraryItemMedia
import com.paulohenriquesg.fahrenheit.api.MediaUpdate
import com.paulohenriquesg.fahrenheit.api.Me
import com.paulohenriquesg.fahrenheit.api.PodcastSettingsApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class ScheduleChoice { Hourly, Daily, Weekly, Custom }

/**
 * The server keeps when to check a show's feed as a cron schedule; the panel
 * offers three plain choices (#182). A schedule the web app built otherwise
 * - every 6 hours, two days a week - is Custom, shown and left alone.
 */
object DownloadSchedule {

    fun choiceOf(cron: String?): ScheduleChoice {
        val fields = cron?.trim()?.split(Regex("\\s+"))?.takeIf { it.size == 5 } ?: return ScheduleChoice.Custom
        val (minute, hour, dayOfMonth, month, dayOfWeek) = fields
        if (minute.inRange(0..59) && dayOfMonth == "*" && month == "*") {
            if (hour == "*" && dayOfWeek == "*") return ScheduleChoice.Hourly
            if (hour.inRange(0..23) && dayOfWeek == "*") return ScheduleChoice.Daily
            if (hour.inRange(0..23) && dayOfWeek.inRange(0..7)) return ScheduleChoice.Weekly
        }
        return ScheduleChoice.Custom
    }

    /** As the web app writes them: on the hour, at midnight, and Sunday at midnight. */
    fun cronOf(choice: ScheduleChoice): String = when (choice) {
        ScheduleChoice.Hourly -> "0 * * * *"
        ScheduleChoice.Daily -> "0 0 * * *"
        ScheduleChoice.Weekly -> "0 0 * * 0"
        ScheduleChoice.Custom -> error("Custom is never picked")
    }

    private fun String.inRange(range: IntRange) = toIntOrNull()?.let { it in range } == true
}

/**
 * A show's own auto-download settings, as the server keeps them on the
 * podcast's media (#182).
 *
 * @property keep how many downloaded episodes the server keeps; 0 keeps all.
 * @property perCheck how many new episodes one check downloads; 0 is no limit.
 */
data class DownloadSettings(val enabled: Boolean, val schedule: String?, val keep: Int, val perCheck: Int) {

    fun with(change: DownloadChange): DownloadSettings = when (change) {
        is DownloadChange.Enabled -> copy(enabled = change.on)
        // A schedule that already reads as the choice is left as it is: a
        // daily check at 04:30 does not move to midnight.
        is DownloadChange.Schedule ->
            if (DownloadSchedule.choiceOf(schedule) == change.choice) this
            else copy(schedule = DownloadSchedule.cronOf(change.choice))
        is DownloadChange.Keep -> copy(keep = change.count)
        is DownloadChange.PerCheck -> copy(perCheck = change.count)
    }

    companion object {
        private val KEEP = listOf(0, 5, 10, 25)
        private val PER_CHECK = listOf(1, 3, 10)

        /** What the server leaves out reads as its own defaults: off, keep all, three per check. */
        fun of(media: LibraryItemMedia): DownloadSettings = DownloadSettings(
            enabled = media.autoDownloadEpisodes == true,
            schedule = media.autoDownloadSchedule,
            keep = media.maxEpisodesToKeep ?: 0,
            perCheck = media.maxNewEpisodesToDownload ?: 3
        )

        /** All, then the latest few; the server's own number among them when it is not one of ours. */
        fun keepOptions(current: Int): List<Int> =
            listOf(0) + (KEEP.drop(1) + current).filter { it > 0 }.distinct().sorted()

        /** A few, then All; the server's own number among them when it is not one of ours. */
        fun perCheckOptions(current: Int): List<Int> =
            (PER_CHECK + current).filter { it > 0 }.distinct().sorted() + 0

        fun keepLabel(value: Int): String = if (value == 0) "All" else "Latest $value"

        fun perCheckLabel(value: Int): String = if (value == 0) "All" else "$value"

        /**
         * Whether the server would take a change: it checks the update right
         * alone (`canUpdate`), whatever the account type, and answers 403
         * without it. An admin has it unless it was turned off.
         */
        fun mayChange(me: Me?): Boolean = me?.permissions?.update == true
    }
}

sealed interface DownloadChange {
    data class Enabled(val on: Boolean) : DownloadChange
    data class Schedule(val choice: ScheduleChoice) : DownloadChange
    data class Keep(val count: Int) : DownloadChange
    data class PerCheck(val count: Int) : DownloadChange
}

/**
 * The panel's settings, saved as each is chosen: the choice shows at once,
 * only its own field is sent, and a refusal puts that field back and says so.
 *
 * Saves go one at a time, in the order chosen, so two quick presses on one
 * setting reach the server in that order (review).
 */
class PodcastDownloads(private val itemId: String, initial: DownloadSettings, private val api: PodcastSettingsApi) {
    var settings by mutableStateOf(initial)
        private set

    /** The last save failed; the next change, or [seen], clears it. */
    var failed by mutableStateOf(false)
        private set

    // What the server last took, and the newest change asked of each field.
    private var saved = initial
    private val newest = mutableMapOf<Class<out DownloadChange>, DownloadChange>()
    private val saving = Mutex()

    /** @return whether it was saved, or there was nothing to save. */
    suspend fun change(change: DownloadChange): Boolean {
        val next = settings.with(change)
        if (next == settings) return true
        failed = false
        settings = next
        newest[change::class.java] = change
        val update = update(change, next)
        return saving.withLock {
            try {
                api.updateMedia(itemId, update)
                saved = saved.restore(change, next)
                true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Only this field, and only if nothing newer was asked of it:
                // a newer choice is on its way and will say for itself.
                if (newest[change::class.java] === change) {
                    settings = settings.restore(change, saved)
                    failed = true
                }
                false
            }
        }
    }

    /** The panel was closed: an old failure is not news when it opens again. */
    fun seen() {
        failed = false
    }

    private fun update(change: DownloadChange, next: DownloadSettings): MediaUpdate = when (change) {
        is DownloadChange.Enabled -> MediaUpdate(autoDownloadEpisodes = next.enabled)
        is DownloadChange.Schedule -> MediaUpdate(autoDownloadSchedule = next.schedule)
        is DownloadChange.Keep -> MediaUpdate(maxEpisodesToKeep = next.keep)
        is DownloadChange.PerCheck -> MediaUpdate(maxNewEpisodesToDownload = next.perCheck)
    }

    /** This settings with [change]'s field taken from [from]. */
    private fun DownloadSettings.restore(change: DownloadChange, from: DownloadSettings) = when (change) {
        is DownloadChange.Enabled -> copy(enabled = from.enabled)
        is DownloadChange.Schedule -> copy(schedule = from.schedule)
        is DownloadChange.Keep -> copy(keep = from.keep)
        is DownloadChange.PerCheck -> copy(perCheck = from.perCheck)
    }
}
