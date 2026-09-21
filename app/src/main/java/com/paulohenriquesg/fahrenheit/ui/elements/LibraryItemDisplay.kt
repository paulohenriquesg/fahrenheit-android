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
}
