package com.paulohenriquesg.fahrenheit.podcast

import com.google.gson.JsonObject
import com.paulohenriquesg.fahrenheit.api.Episode

enum class EpisodeTab { All, OnServer, NotDownloaded }

/**
 * One episode on a podcast's screen: in the feed, on the server, or both.
 *
 * @property feed the feed's own JSON for the episode, exactly as the server
 *   sent it. Downloading posts it straight back, so nothing is lost by passing
 *   it through a model of ours.
 */
data class EpisodeRow(
    val key: String,
    val title: String,
    val publishedAt: Long?,
    val durationSeconds: Double?,
    val description: String?,
    val onServer: Episode?,
    val feed: JsonObject?
) {
    val downloaded: Boolean get() = onServer != null
}

/** Every episode in the feed, and whether the server has it (#76). */
object EpisodeList {

    /**
     * @param feed the feed's episodes, or null when there is no feed to read -
     *   not an admin, or it failed - in which case the list is the server's.
     */
    fun merge(server: List<Episode>, feed: List<JsonObject>?): List<EpisodeRow> {
        val unmatched = server.toMutableList()
        val fromFeed = feed.orEmpty().mapIndexed { index, json ->
            val match = unmatched.firstOrNull { matches(it, json) }?.also { unmatched.remove(it) }
            row(json, index, match)
        }
        // Episodes the feed has dropped are still on the server, still playable.
        val serverOnly = unmatched.map { row(it) }
        return (fromFeed + serverOnly).sortedByDescending { it.publishedAt ?: 0L }
    }

    fun filter(rows: List<EpisodeRow>, tab: EpisodeTab): List<EpisodeRow> = when (tab) {
        EpisodeTab.All -> rows
        EpisodeTab.OnServer -> rows.filter { it.downloaded }
        EpisodeTab.NotDownloaded -> rows.filterNot { it.downloaded }
    }

    /**
     * The server's rule: the same guid, or else the same enclosure URL. A guid
     * missing on both sides is not a match, which a plain equality would make it.
     */
    fun matches(episode: Episode, feed: JsonObject): Boolean {
        val guid = feed.string("guid")
        if (episode.guid != null && episode.guid == guid) return true
        val url = feed.getAsJsonObject("enclosure")?.string("url")
        return episode.enclosure?.url != null && episode.enclosure.url == url
    }

    private fun row(json: JsonObject, index: Int, onServer: Episode?) = EpisodeRow(
        key = onServer?.let { "server:${it.id}" }
            ?: "feed:${json.string("guid") ?: json.getAsJsonObject("enclosure")?.string("url") ?: index}",
        title = onServer?.title ?: json.string("title").orEmpty(),
        publishedAt = json.long("publishedAt") ?: onServer?.publishedAt,
        durationSeconds = onServer?.duration ?: json.double("durationSeconds"),
        description = onServer?.description ?: json.string("description"),
        onServer = onServer,
        feed = json
    )

    private fun row(episode: Episode) = EpisodeRow(
        key = "server:${episode.id}",
        title = episode.title,
        publishedAt = episode.publishedAt.takeIf { it > 0 },
        durationSeconds = episode.duration,
        description = episode.description,
        onServer = episode,
        feed = null
    )

    private fun JsonObject.string(name: String): String? =
        get(name)?.takeUnless { it.isJsonNull }?.asString?.takeIf { it.isNotEmpty() }

    private fun JsonObject.long(name: String): Long? =
        get(name)?.takeUnless { it.isJsonNull }?.let { runCatching { it.asLong }.getOrNull() }

    private fun JsonObject.double(name: String): Double? =
        get(name)?.takeUnless { it.isJsonNull }?.let { runCatching { it.asDouble }.getOrNull() }
}
