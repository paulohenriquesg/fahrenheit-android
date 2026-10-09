package com.paulohenriquesg.fahrenheit.podcast

import com.google.gson.JsonObject
import com.paulohenriquesg.fahrenheit.api.DownloadQueue
import com.paulohenriquesg.fahrenheit.api.Episode
import com.paulohenriquesg.fahrenheit.api.PodcastApi
import android.util.Log
import com.paulohenriquesg.fahrenheit.BuildConfig
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
    private val podcastId: String,
    private val now: () -> Long = System::currentTimeMillis
) {
    /**
     * Episodes asked for here and not yet on the server, by row key. One that
     * has been nowhere to be seen for long reads as failed but stays here, so
     * that turning up late still turns its row downloaded (#215).
     */
    private val pending = mutableMapOf<String, JsonObject>()

    /** When each was asked for. */
    private val asked = mutableMapOf<String, Long>()

    /** Asks the server to download [episode]; false when it refused. */
    suspend fun request(key: String, episode: JsonObject): Boolean {
        val accepted = runCatching { api.downloadEpisodes(podcastId, listOf(episode)) }.isSuccess
        if (accepted) {
            pending[key] = episode
            asked[key] = now()
        }
        return accepted
    }

    /** Whether there is anything a running [watch] would still be waiting for. */
    val waiting: Boolean get() = pending.isNotEmpty()

    /**
     * Polls until nothing for this podcast is queued, downloading or awaited;
     * while something is awaited the podcast is read again each time, as the
     * server's queue has been seen empty while it fetched (#215).
     *
     * @param onUpdate the latest queue, and the episodes asked for and awaited.
     * @param reload the podcast's episodes, fetched again; null on failure.
     */
    suspend fun watch(
        onUpdate: (DownloadQueue, Map<String, DownloadRequest>) -> Unit,
        pause: suspend () -> Unit = { delay(POLL_INTERVAL_MS) },
        reload: suspend () -> List<Episode>?
    ) {
        var wasBusy = false
        repeat(MAX_POLLS) { poll ->
            if (poll > 0) pause()
            // A queue that cannot be read this time is not the end of the watch.
            val queue = runCatching { api.downloadQueue(libraryId) }.getOrNull() ?: return@repeat
            // #215: the reply was seen empty mid-download on 2.37.1; say what it was.
            if (BuildConfig.DEBUG) Log.d(TAG, "episode-downloads for $libraryId: $queue")
            val busy = DownloadProgress.busy(queue, podcastId)

            // Something may have landed since the last look.
            val episodes = if (wasBusy || pending.isNotEmpty()) reload() else null
            pending.entries.removeAll { (key, feed) ->
                (episodes?.any { EpisodeList.matches(it, feed) } == true).also { landed -> if (landed) asked.remove(key) }
            }
            onUpdate(queue, asked.mapValues { DownloadRequest.Asked(it.value) })

            if (!busy && pending.isEmpty()) return
            wasBusy = busy
        }
    }

    private companion object {
        const val TAG = "DownloadWatch"
        const val POLL_INTERVAL_MS = 5_000L

        /** An hour: a long queue of other podcasts' episodes can take that. */
        const val MAX_POLLS = 720
    }
}
