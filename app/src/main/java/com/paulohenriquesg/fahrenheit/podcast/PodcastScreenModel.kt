package com.paulohenriquesg.fahrenheit.podcast

import com.google.gson.JsonObject
import com.paulohenriquesg.fahrenheit.api.Episode
import com.paulohenriquesg.fahrenheit.utils.EpisodeDate

sealed interface FeedLoad {
    data object Unavailable : FeedLoad
    data object Loading : FeedLoad
    data object Failed : FeedLoad
    data class Loaded(val episodes: List<JsonObject>) : FeedLoad
}

data class Fact(val text: String, val warn: Boolean = false)
data class Note(val title: String? = null, val body: String)

data class PodcastScreen(
    val facts: List<Fact>,
    val tabs: List<Pair<EpisodeTab, Int>>?,
    val rows: List<EpisodeRow>,
    val note: Note?
)

/**
 * What a podcast's screen says (#75, #76): the facts at the top, the tabs, the
 * rows, and a note when the list is not the whole story.
 *
 * The library only holds what the server has downloaded, so an empty podcast
 * is almost never an empty feed. These facts are what explain it.
 */
object PodcastScreenModel {

    fun of(
        server: List<Episode>,
        feed: FeedLoad,
        tab: EpisodeTab,
        lastEpisodeCheck: Long?,
        autoDownload: Boolean?,
        now: Long,
        serverFormat: String? = null,
        schedule: String? = null
    ): PodcastScreen {
        val feedEpisodes = (feed as? FeedLoad.Loaded)?.episodes
        val all = EpisodeList.merge(server, feedEpisodes)
        val onServer = all.count { it.downloaded }
        // Tabs only when there are two kinds of row to tell apart.
        val tabs = feedEpisodes?.let {
            listOf(
                EpisodeTab.All to all.size,
                EpisodeTab.OnServer to onServer,
                EpisodeTab.NotDownloaded to all.size - onServer
            )
        }
        return PodcastScreen(
            facts = listOfNotNull(
                held(onServer, feedEpisodes?.let { all.size }),
                checked(lastEpisodeCheck, now, serverFormat),
                when (autoDownload) {
                    true -> when (DownloadSchedule.choiceOf(schedule)) {
                        ScheduleChoice.Hourly -> Fact("New episodes download every hour")
                        ScheduleChoice.Daily -> Fact("New episodes download every day")
                        ScheduleChoice.Weekly -> Fact("New episodes download every week")
                        ScheduleChoice.Custom -> Fact("Automatic downloads on")
                    }
                    false -> Fact("Automatic downloads off", warn = true)
                    null -> null
                }
            ),
            tabs = tabs,
            rows = if (tabs == null) all else EpisodeList.filter(all, tab),
            note = note(feed, all.isEmpty())
        )
    }

    private fun held(onServer: Int, inFeed: Int?): Fact = when {
        inFeed != null -> Fact("$onServer of $inFeed on the server")
        onServer == 0 -> Fact("Nothing downloaded yet")
        onServer == 1 -> Fact("1 episode on the server")
        else -> Fact("$onServer episodes on the server")
    }

    // The server stores "never" as 0 as well as null.
    private fun checked(at: Long?, now: Long, serverFormat: String?): Fact =
        if (at == null || at <= 0) {
            Fact("Feed never checked")
        } else {
            Fact("Feed last checked: ${EpisodeDate.of(at, now, serverFormat)}")
        }

    private fun note(feed: FeedLoad, empty: Boolean): Note? = when (feed) {
        is FeedLoad.Loaded -> null
        FeedLoad.Loading -> Note(body = "Reading the feed…")
        FeedLoad.Failed -> Note(body = "Could not read the feed. Showing what the server has.")
        FeedLoad.Unavailable ->
            if (empty) {
                Note(
                    title = "Nothing downloaded yet",
                    body = "The server holds no episodes of this podcast. An admin can fetch them from the feed."
                )
            } else {
                Note(body = "Only episodes the server has downloaded are listed. An admin can fetch more from the feed.")
            }
    }
}
