package com.paulohenriquesg.fahrenheit.podcast

import com.google.gson.JsonObject
import com.paulohenriquesg.fahrenheit.api.DownloadQueue
import com.paulohenriquesg.fahrenheit.api.Episode
import com.paulohenriquesg.fahrenheit.api.PodcastApi
import kotlinx.coroutines.delay

/**
 * Watches the server's download queue while this podcast has something in it,
 * so a row can say "waiting", "downloading" or "failed", and turn playable when
 * its file lands (#76 step 3).
 *
 * One per podcast screen, run in that screen's scope: leaving the screen stops it.
 */
class DownloadWatch(
    private val api: PodcastApi,
    private val libraryId: String,
    private val podcastId: String
) {
    /** Episodes asked for here and not yet landed or given up on, by row key. */
    private val pending = mutableMapOf<String, JsonObject>()

    /** Polls in a row each requested episode has been nowhere to be seen. */
    private val misses = mutableMapOf<String, Int>()

    /** Asks the server to download [episode]; false when it refused. */
    suspend fun request(key: String, episode: JsonObject): Boolean {
        val accepted = runCatching { api.downloadEpisodes(podcastId, listOf(episode)) }.isSuccess
        if (accepted) {
            pending[key] = episode
            misses[key] = 0
        }
        return accepted
    }

    /** Whether there is anything a running [watch] would still be waiting for. */
    val waiting: Boolean get() = pending.isNotEmpty()

    /**
     * Polls until nothing for this podcast is queued, downloading or awaited.
     *
     * @param onUpdate the latest queue, and the misses of requested episodes.
     * @param reload the podcast's episodes, fetched again; null on failure.
     */
    suspend fun watch(
        onUpdate: (DownloadQueue, Map<String, Int>) -> Unit,
        pause: suspend () -> Unit = { delay(POLL_INTERVAL_MS) },
        reload: suspend () -> List<Episode>?
    ) {
        var wasBusy = false
        repeat(MAX_POLLS) { poll ->
            if (poll > 0) pause()
            // A queue that cannot be read this time is not the end of the watch.
            val queue = runCatching { api.downloadQueue(libraryId) }.getOrNull() ?: return@repeat
            val busy = DownloadProgress.busy(queue, podcastId)

            // Something may have landed since the last look.
            val episodes = if (wasBusy || pending.isNotEmpty()) reload() else null
            pending.entries.removeAll { (key, feed) ->
                val landed = episodes?.any { EpisodeList.matches(it, feed) } == true
                val inQueue = DownloadProgress.queued(queue, feed)
                when {
                    landed -> { misses.remove(key); true }
                    inQueue -> { misses[key] = 0; false }
                    else -> {
                        val missed = (misses[key] ?: 0) + 1
                        misses[key] = missed
                        missed >= DownloadProgress.MISSES_BEFORE_FAILED
                    }
                }
            }
            onUpdate(queue, misses.toMap())

            if (!busy && pending.isEmpty()) return
            wasBusy = busy
        }
    }

    private companion object {
        const val POLL_INTERVAL_MS = 5_000L

        /** An hour: a long queue of other podcasts' episodes can take that. */
        const val MAX_POLLS = 720
    }
}
