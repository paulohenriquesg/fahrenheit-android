package com.paulohenriquesg.fahrenheit.favourites

import com.paulohenriquesg.fahrenheit.api.NewPlaylist
import com.paulohenriquesg.fahrenheit.api.Playlist
import com.paulohenriquesg.fahrenheit.api.PlaylistApi
import com.paulohenriquesg.fahrenheit.api.PlaylistItem
import com.paulohenriquesg.fahrenheit.api.PlaylistItems
import com.paulohenriquesg.fahrenheit.api.PlaylistsResponse
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response

/**
 * The server's playlists, kept as it keeps them: per library, in order, and
 * deleted when the last item is removed (PlaylistController.removeBatch).
 */
class FakePlaylists : PlaylistApi {
    val playlists = mutableListOf<Playlist>()
    var failing: Throwable? = null
    /** Fails create alone, after anything else has gone through. */
    var failingCreate: Throwable? = null
    val calls = mutableListOf<String>()
    private var nextId = 100

    fun add(id: String, name: String, libraryId: String = "lib_1", vararg items: PlaylistItem): Playlist =
        Playlist(id, name, libraryId, items.toList()).also { playlists += it }

    private fun fail() = failing?.let { throw it }

    private fun find(id: String): Playlist = playlists.find { it.id == id } ?: throw notFound()

    private fun replace(updated: Playlist): Playlist {
        playlists.replaceAll { if (it.id == updated.id) updated else it }
        return updated
    }

    override suspend fun playlists(libraryId: String): PlaylistsResponse {
        calls += "list $libraryId"; fail()
        return PlaylistsResponse(playlists.filter { it.libraryId == libraryId })
    }

    override suspend fun playlist(id: String): Playlist {
        calls += "get $id"; fail()
        return find(id)
    }

    override suspend fun create(body: NewPlaylist): Playlist {
        calls += "create ${body.name}"; fail(); failingCreate?.let { throw it }
        return add("pl_${nextId++}", body.name, body.libraryId)
    }

    override suspend fun addItems(id: String, body: PlaylistItems): Playlist {
        calls += "add $id"; fail()
        val playlist = find(id)
        val added = body.items
            .filter { ref -> playlist.items.orEmpty().none { it.libraryItemId == ref.libraryItemId && it.episodeId == ref.episodeId } }
            .map { PlaylistItem(it.libraryItemId, it.episodeId) }
        return replace(playlist.copy(items = playlist.items.orEmpty() + added))
    }

    override suspend fun removeItems(id: String, body: PlaylistItems): Playlist {
        calls += "remove $id"; fail()
        val playlist = find(id)
        val left = playlist.items.orEmpty().filter { item ->
            body.items.none { it.libraryItemId == item.libraryItemId && it.episodeId == item.episodeId }
        }
        val updated = replace(playlist.copy(items = left))
        if (left.isEmpty()) playlists.remove(updated)
        return updated
    }

    companion object {
        fun notFound() = HttpException(Response.error<Any>(404, "Playlist not found".toResponseBody()))
    }
}

/** A choice kept in memory. */
class MemoryChoice(vararg chosen: Pair<String, String>) : FavouritesStore {
    val chosen = mutableMapOf(*chosen)
    override fun playlistFor(libraryId: String): String? = chosen[libraryId]
    override fun choose(libraryId: String, playlistId: String?) {
        if (playlistId == null) chosen.remove(libraryId) else chosen[libraryId] = playlistId
    }
}
