package com.paulohenriquesg.fahrenheit.favourites

import android.content.Context
import androidx.core.content.edit

/** Which playlist is Favourites, per library; null for None (#180). */
interface FavouritesStore {
    fun playlistFor(libraryId: String): String?
    fun choose(libraryId: String, playlistId: String?)
}

/**
 * The choice, on this device. The playlist itself is on the server; only which
 * one is kept here, one per library, since the server keeps playlists per library.
 */
class FavouritesChoice(context: Context) : FavouritesStore {
    private val prefs = context.getSharedPreferences("favourites", Context.MODE_PRIVATE)

    override fun playlistFor(libraryId: String): String? = prefs.getString(key(libraryId), null)

    override fun choose(libraryId: String, playlistId: String?) = prefs.edit {
        if (playlistId == null) remove(key(libraryId)) else putString(key(libraryId), playlistId)
    }

    private fun key(libraryId: String) = "playlist_$libraryId"
}
