package com.paulohenriquesg.fahrenheit.podcast

import com.paulohenriquesg.fahrenheit.api.PodcastApi
import kotlinx.coroutines.delay

/**
 * "Check for new episodes": asks the server to look at a podcast's feed and
 * download what is new (#76).
 *
 * Offered only where the server would honour it ([mayCheck]). A button that
 * answers 403 for an ordinary account is worse than no button, so anything
 * short of knowing the user is an admin means not offering it.
 */
class FeedCheck(private val api: PodcastApi) {

    /** How many new episodes the server found and queued for download. */
    suspend fun check(podcastId: String): Result<Int> = runCatching {
        api.checkNew(podcastId, LIMIT).episodes.orEmpty().size
    }

    /**
     * The button press, start to finish: check, report, then reload the podcast
     * until what was found has downloaded.
     *
     * @param reload fetches the podcast again and returns its episode count,
     *   or null when the fetch failed.
     */
    suspend fun run(
        podcastId: String,
        episodesBefore: Int,
        onState: (FeedCheckState) -> Unit,
        pause: suspend () -> Unit = { delay(POLL_INTERVAL_MS) },
        reload: suspend () -> Int?
    ) {
        onState(FeedCheckState.Checking)
        val found = check(podcastId).getOrElse {
            onState(FeedCheckState.Failed)
            return
        }
        onState(FeedCheckState.Found(found))
        if (found > 0) {
            awaitEpisodes(episodesBefore + found, POLL_ATTEMPTS, pause, reload)
        }
    }

    companion object {
        /**
         * Whether to offer the check, from the user's type in GET /api/me (null
         * when that could not be read). Mirrors the server's isAdminOrUp, and
         * needs a feed: otherwise the server answers 403 or 400.
         */
        fun mayCheck(type: String?, feedUrl: String?): Boolean =
            !feedUrl.isNullOrBlank() && (type == "admin" || type == "root")

        /**
         * Always sent. The server treats 0 as "no limit", and a podcast unchecked
         * for two years can have dozens of episodes waiting - each one disk on
         * the server. Three is the server's own default.
         */
        const val LIMIT = 3

        /** Two minutes of looking; past that, reopening the screen will show them. */
        private const val POLL_INTERVAL_MS = 5_000L
        private const val POLL_ATTEMPTS = 24
    }
}

/**
 * Reloads until the podcast holds [target] episodes, or [attempts] runs out.
 *
 * The check returns as soon as downloads are queued, not when they finish, so
 * without this the new episodes would only show up the next time the screen
 * was opened. A failed reload (null) is not an answer, just another wait.
 *
 * @return whether the episodes arrived.
 */
suspend fun awaitEpisodes(
    target: Int,
    attempts: Int,
    pause: suspend () -> Unit,
    reload: suspend () -> Int?
): Boolean {
    repeat(attempts) { attempt ->
        if (attempt > 0) pause()
        val count = reload()
        if (count != null && count >= target) return true
    }
    return false
}
