package com.paulohenriquesg.fahrenheit.api

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * The user's playlists (#180), as the server's PlaylistController and
 * LibraryController serve them. Playlists belong to one user and one library,
 * and hold books or podcast episodes.
 *
 * Adding and removing go through the batch endpoints, as the web app's do: an
 * item already in, or already out, is skipped rather than refused.
 */
interface PlaylistApi {
    @GET("api/libraries/{libraryId}/playlists")
    suspend fun playlists(@Path("libraryId") libraryId: String): PlaylistsResponse

    /** 404 when it has been deleted; 403 when it is not this user's. */
    @GET("api/playlists/{id}")
    suspend fun playlist(@Path("id") id: String): Playlist

    @POST("api/playlists")
    suspend fun create(@Body body: NewPlaylist): Playlist

    @POST("api/playlists/{id}/batch/add")
    suspend fun addItems(@Path("id") id: String, @Body body: PlaylistItems): Playlist

    /** The server deletes the playlist when this leaves it empty, and still answers with it. */
    @POST("api/playlists/{id}/batch/remove")
    suspend fun removeItems(@Path("id") id: String, @Body body: PlaylistItems): Playlist
}

data class PlaylistsResponse(@SerializedName("results") val results: List<Playlist>? = null)

data class Playlist(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("libraryId") val libraryId: String? = null,
    /** In the playlist's own order. */
    @SerializedName("items") val items: List<PlaylistItem>? = null
)

/** A book (no [episodeId]) or a podcast episode. */
data class PlaylistItem(
    @SerializedName("libraryItemId") val libraryItemId: String,
    @SerializedName("episodeId") val episodeId: String? = null,
    @SerializedName("episode") val episode: PlaylistEpisode? = null,
    /** Expanded for a book, minified for an episode's show. */
    @SerializedName("libraryItem") val libraryItem: LibraryItem? = null
)

data class PlaylistEpisode(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String? = null,
    @SerializedName("duration") val duration: Double? = null
)

data class NewPlaylist(@SerializedName("libraryId") val libraryId: String, @SerializedName("name") val name: String)

data class PlaylistItems(@SerializedName("items") val items: List<PlaylistItemRef>)

data class PlaylistItemRef(
    @SerializedName("libraryItemId") val libraryItemId: String,
    @SerializedName("episodeId") val episodeId: String? = null
)
