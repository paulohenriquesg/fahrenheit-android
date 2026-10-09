package com.paulohenriquesg.fahrenheit.podcast

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Which tab a show's screen is on, and what its Favourites tab keeps listed
 * (#180): the episodes in the playlist when it was chosen, so one taken out
 * there stays until the tab is chosen again.
 */
@Stable
class EpisodeTabChoice {
    var tab by mutableStateOf(EpisodeTab.All)
        private set

    /** For [PodcastScreenModel.of]'s favouritesKept. */
    var kept by mutableStateOf(emptySet<String>())
        private set

    /** @param inFavourites this show's episodes in the playlist now; null for None. */
    fun choose(tab: EpisodeTab, inFavourites: Set<String>?) {
        if (tab == EpisodeTab.Favourites) kept = inFavourites.orEmpty()
        this.tab = tab
    }

    /**
     * With None, a screen on Favourites goes back to All for good, rather than
     * snapping back to an old filter should a playlist come back.
     */
    fun follow(inFavourites: Set<String>?) {
        if (inFavourites != null || tab != EpisodeTab.Favourites) return
        tab = EpisodeTab.All
        kept = emptySet()
    }
}
