package com.paulohenriquesg.fahrenheit.favourites

import com.paulohenriquesg.fahrenheit.api.NewPlaylist
import com.paulohenriquesg.fahrenheit.api.Playlist
import com.paulohenriquesg.fahrenheit.api.PlaylistApi
import com.paulohenriquesg.fahrenheit.api.PlaylistItemRef
import com.paulohenriquesg.fahrenheit.api.PlaylistItems
import retrofit2.HttpException
import java.util.Locale

/** What the Settings panel offers (frame 0 of docs/mocks/podcast-actions.html). */
sealed interface FavouritesPick {
    data object None : FavouritesPick
    /** A new, empty playlist named [Favourites.NAME], made and chosen. */
    data object Create : FavouritesPick
    data class Existing(val playlist: Playlist) : FavouritesPick
}

/**
 * Favourites (#180): an Audiobookshelf playlist the user chooses per library,
 * so it shows in the web and phone apps too. Until one is chosen there is no
 * heart and no shelf; a chosen one gone from the server is None again.
 */
class Favourites(private val api: PlaylistApi, private val store: FavouritesStore) {

    /** The chosen playlist, as the server has it now; null for None. Fails offline. */
    suspend fun current(libraryId: String): Result<Playlist?> {
        val id = store.playlistFor(libraryId) ?: return Result.success(null)
        return runCatching { api.playlist(id) }.recoverCatching { error ->
            if (error !is HttpException || error.code() !in GONE) throw error
            // Deleted, or not this user's: whoever signed in has no such playlist.
            store.choose(libraryId, null)
            null
        }
    }

    /** The library's playlists, by name. */
    suspend fun playlists(libraryId: String): Result<List<Playlist>> = runCatching {
        api.playlists(libraryId).results.orEmpty().sortedBy { it.name.lowercase(Locale.ROOT) }
    }

    /** Keeps [pick] as the library's Favourites, and answers with the playlist, or null for None. */
    suspend fun choose(libraryId: String, pick: FavouritesPick): Result<Playlist?> = runCatching {
        val playlist = when (pick) {
            FavouritesPick.None -> null
            FavouritesPick.Create -> api.create(NewPlaylist(libraryId, NAME))
            is FavouritesPick.Existing -> pick.playlist
        }
        store.choose(libraryId, playlist?.id)
        playlist
    }

    /** Adds the book or episode to [playlist], or takes it out; the playlist after. */
    suspend fun toggle(playlist: Playlist, itemId: String, episodeId: String?): Result<Playlist> = runCatching {
        val items = PlaylistItems(listOf(PlaylistItemRef(itemId, episodeId)))
        if (!playlist.holds(itemId, episodeId)) return@runCatching api.addItems(playlist.id, items)
        val after = api.removeItems(playlist.id, items)
        if (!after.items.isNullOrEmpty()) return@runCatching after
        // The server deleted it with its last item. Taking out the last
        // favourite is not turning Favourites off: the same name, empty, stays chosen.
        val libraryId = playlist.libraryId ?: return@runCatching after
        api.create(NewPlaylist(libraryId, playlist.name)).also { store.choose(libraryId, it.id) }
    }

    companion object {
        const val NAME = "Favourites"
        private val GONE = setOf(403, 404)

        /** Create is not offered beside a playlist already called Favourites: that one is listed. */
        fun offersCreate(playlists: List<Playlist>): Boolean = playlists.none { it.name.equals(NAME, ignoreCase = true) }
    }
}

/** Whether the book ([episodeId] null) or the episode is in this playlist. */
fun Playlist.holds(itemId: String, episodeId: String?): Boolean = items.orEmpty().any {
    if (episodeId != null) it.episodeId == episodeId else it.episodeId == null && it.libraryItemId == itemId
}
