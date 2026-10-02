package com.paulohenriquesg.fahrenheit.ui.elements

import com.paulohenriquesg.fahrenheit.api.LibraryItem

/** What a card on the library grid shows. */
object LibraryItemDisplay {

    private const val MAX_BADGE_COUNT = 99

    /**
     * A podcast leads with its newest episode, since that is what changes and
     * what the listener is looking for; everything else shows its own title.
     */
    fun title(item: LibraryItem): String =
        if (item.mediaType == "podcast" && item.recentEpisode != null) {
            item.recentEpisode.title ?: item.media.metadata.title
        } else {
            item.media.metadata.title
        }

    /**
     * The count of episodes still to hear, or null when there is no badge to
     * draw. A podcast that is fully heard reports 0, which used to be drawn as
     * a red "0" on the cover.
     */
    fun unfinishedBadge(item: LibraryItem): String? {
        if (item.mediaType != "podcast") return null
        val unfinished = item.numEpisodesIncomplete ?: return null
        if (unfinished <= 0) return null
        return if (unfinished > MAX_BADGE_COUNT) "${MAX_BADGE_COUNT}+" else unfinished.toString()
    }

    /**
     * How many episodes the server holds, so a podcast with nothing downloaded
     * can be told apart before it is opened (#75). Null for books, and when the
     * server sent no count - a missing count is not a zero.
     *
     * Null too when the card stands for an episode, as on the Home shelves: the
     * line would describe the podcast rather than the card, and the server
     * sends 0 there for every podcast anyway.
     */
    fun episodeCount(item: LibraryItem): String? {
        if (item.mediaType != "podcast" || item.recentEpisode != null) return null
        return when (val count = item.media.numEpisodes ?: return null) {
            0 -> "Nothing downloaded"
            1 -> "1 episode"
            else -> "$count episodes"
        }
    }

    /**
     * Whether the card is drawn dimmed: a podcast the server holds nothing of,
     * so the ones with something in them stand out (#75). The same podcasts
     * whose count line says "Nothing downloaded".
     */
    fun dimmed(item: LibraryItem): Boolean =
        item.mediaType == "podcast" && item.recentEpisode == null && item.media.numEpisodes == 0
}
