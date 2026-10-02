package com.paulohenriquesg.fahrenheit.podcast

import com.paulohenriquesg.fahrenheit.utils.EpisodeDate

/**
 * What a podcast with nothing downloaded says instead of "No Episodes" (#75).
 *
 * The library holds only what the server has downloaded, so an empty podcast
 * is almost never an empty feed: on the test server, one with 56 episodes in
 * its feed had not been checked for 705 days, because automatic downloads were
 * off. These are the facts that explain it, all from `/api/items/{id}`.
 */
object EmptyPodcast {

    fun lines(
        lastEpisodeCheck: Long?,
        autoDownload: Boolean?,
        now: Long,
        serverFormat: String? = null
    ): List<String> = listOfNotNull(
        "Nothing downloaded yet",
        // The server stores "never" as 0 as well as null.
        if (lastEpisodeCheck == null || lastEpisodeCheck <= 0) {
            "Feed never checked"
        } else {
            "Feed last checked: ${EpisodeDate.of(lastEpisodeCheck, now, serverFormat)}"
        },
        when (autoDownload) {
            true -> "New episodes download automatically"
            false -> "Automatic downloads are off for this podcast"
            null -> null
        }
    )
}
