package com.paulohenriquesg.fahrenheit.favourites

import com.google.gson.Gson
import com.paulohenriquesg.fahrenheit.api.LibraryItem
import com.paulohenriquesg.fahrenheit.api.Playlist
import com.paulohenriquesg.fahrenheit.api.RecentEpisode
import com.paulohenriquesg.fahrenheit.api.Shelf

/**
 * Home's Favourites shelf (#180; frame 4 of docs/mocks/podcast-actions.html):
 * the chosen playlist under its own name, in its own order, after Continue
 * Listening. Drawn as the server's shelves are (see HomeShelves): an episode
 * shelf plays an episode from where it was left, a book shelf opens the book.
 */
object FavouritesShelf {
    const val ID = "favourites"
    private const val CONTINUE_LISTENING = "continue-listening"

    /** The shelf for [playlist], or null for None or an empty playlist. */
    fun of(playlist: Playlist?): Shelf? {
        val items = playlist?.items.orEmpty()
        val entities = items.mapNotNull { item ->
            val libraryItem = item.libraryItem ?: return@mapNotNull null
            val episodeId = item.episodeId ?: return@mapNotNull libraryItem
            // The shape the server's episode shelves have: the show, with its episode.
            withEpisode(libraryItem, RecentEpisode(episodeId, item.episode?.title, item.libraryItemId))
        }
        if (playlist == null || entities.isEmpty()) return null
        // A playlist holds books or episodes, never both: a library holds one or the other.
        val type = if (entities.any { it.recentEpisode != null }) "episode" else "book"
        return Shelf(id = ID, label = playlist.name, labelStringKey = "", type = type, bookEntities = entities)
    }

    /**
     * Through Gson rather than `copy`: Gson leaves a field the server left out
     * null whatever its Kotlin type, and `copy` throws on that null.
     */
    private fun withEpisode(item: LibraryItem, episode: RecentEpisode): LibraryItem {
        val tree = gson.toJsonTree(item).asJsonObject
        tree.add("recentEpisode", gson.toJsonTree(episode))
        return gson.fromJson(tree, LibraryItem::class.java)
    }

    private val gson = Gson()

    /** Home's [shelves] with the library's Favourites among them; as they were with None or offline. */
    suspend fun onto(shelves: List<Shelf>, libraryId: String, favourites: Favourites?): List<Shelf> =
        placed(shelves, of(favourites?.current(libraryId)?.getOrNull()))

    /**
     * Home's shelves with the Favourites shelf read again, as after the player;
     * as they were offline. [shelves] is asked for after the read, so a load of
     * Home that lands meanwhile is not overwritten with the list from before it.
     */
    suspend fun refreshed(shelves: () -> List<Shelf>, libraryId: String, favourites: Favourites?): List<Shelf> {
        val read = favourites?.current(libraryId) ?: return shelves().filter { it.id != ID }
        val playlist = read.getOrElse { return shelves() }
        return placed(shelves().filter { it.id != ID }, of(playlist))
    }

    /** [shelves] with [favourites] after Continue Listening, or first without it. */
    fun placed(shelves: List<Shelf>, favourites: Shelf?): List<Shelf> {
        if (favourites == null) return shelves
        val at = shelves.indexOfFirst { it.id == CONTINUE_LISTENING } + 1
        return shelves.take(at) + favourites + shelves.drop(at)
    }
}
