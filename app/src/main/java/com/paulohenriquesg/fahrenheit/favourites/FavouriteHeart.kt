package com.paulohenriquesg.fahrenheit.favourites

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import com.paulohenriquesg.fahrenheit.R
import com.paulohenriquesg.fahrenheit.api.Playlist
import com.paulohenriquesg.fahrenheit.player.TransportButton
import com.paulohenriquesg.fahrenheit.ui.elements.RowButton

const val FAVOURITE_BUTTON_TAG = "player_favourite"

/** What a press on the heart did, for the short note that confirms it. */
sealed interface HeartChange {
    data class Added(val playlist: String) : HeartChange
    data class Removed(val playlist: String) : HeartChange
    data object Failed : HeartChange
}

/** The short note a press confirms with: "Added to Bedtime", or why nothing changed. */
fun HeartChange.note(context: Context): String = when (this) {
    is HeartChange.Added -> context.getString(R.string.favourite_added, playlist)
    is HeartChange.Removed -> context.getString(R.string.favourite_removed, playlist)
    HeartChange.Failed -> context.getString(R.string.favourites_change_failed)
}

/**
 * One library's Favourites playlist, as last read, for every heart on a screen
 * (#180): the player's one, or one on each episode row (frame 2).
 */
@Stable
class LibraryFavourites(private val favourites: Favourites, private val libraryId: String) {

    /** The library's Favourites playlist; null for None, or not read yet. */
    var playlist by mutableStateOf<Playlist?>(null)
        private set

    private var busy = false

    /** Bumped by each press, so a read sent before one cannot undo it when it answers. */
    private var presses = 0

    /** Reads it again; offline, it stays as it was rather than taking the hearts away. */
    suspend fun load() {
        val before = presses
        val read = favourites.current(libraryId)
        if (presses != before || busy) return
        playlist = read.getOrElse { playlist }
    }

    fun holds(itemId: String, episodeId: String?): Boolean = playlist?.holds(itemId, episodeId) == true

    /** The episodes of [itemId] in the playlist; null for None. */
    fun episodesOf(itemId: String): Set<String>? = playlist?.let { chosen ->
        chosen.items.orEmpty().filter { it.libraryItemId == itemId }.mapNotNull { it.episodeId }.toSet()
    }

    /**
     * Adds or takes out; on a failure the playlist is as the server now has
     * it, or as it was offline. Null for a press while the last is still on
     * its way.
     */
    suspend fun toggle(itemId: String, episodeId: String?): HeartChange? {
        val before = playlist ?: return null
        if (busy) return null
        busy = true
        presses++
        val wasIn = holds(itemId, episodeId)
        return try {
            favourites.toggle(before, itemId, episodeId).fold(
                onSuccess = { after ->
                    playlist = after
                    if (wasIn) HeartChange.Removed(after.name) else HeartChange.Added(after.name)
                },
                onFailure = {
                    // Read it again: offline it stays as it was, but a playlist
                    // that has gone (deleted elsewhere, or with its last item and
                    // not made again) takes the hearts away rather than failing forever.
                    playlist = favourites.current(libraryId).getOrElse { before }
                    HeartChange.Failed
                }
            )
        } finally {
            busy = false
        }
    }
}

/**
 * The heart for one book or episode (#180): there only while the library has
 * a Favourites playlist, filled while this is in it. A press that fails leaves
 * it as it was.
 */
@Stable
class FavouriteHeart(
    favourites: Favourites,
    libraryId: String,
    private val itemId: String,
    private val episodeId: String?
) {
    private val library = LibraryFavourites(favourites, libraryId)

    /** The library's Favourites playlist; null for None, or not read yet. */
    val playlist: Playlist? get() = library.playlist

    val filled: Boolean get() = library.holds(itemId, episodeId)

    suspend fun load() = library.load()

    /** See [LibraryFavourites.toggle]. */
    suspend fun toggle(): HeartChange? = library.toggle(itemId, episodeId)
}

/**
 * The heart among the player's actions (frame 1): a round icon button, the
 * transport's own, filled while this is in [playlist]. No label, so the
 * actions beside it keep their width on a 1920-wide screen.
 */
@Composable
fun FavouriteButton(filled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, playlist: String = Favourites.NAME) {
    TransportButton(
        onClick = onClick,
        size = 48.dp,
        container = MaterialTheme.colorScheme.secondaryContainer,
        content = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = modifier.testTag(FAVOURITE_BUTTON_TAG)
    ) {
        Icon(heartIcon(filled), contentDescription = heartDescription(filled, playlist))
    }
}

/**
 * The heart on a focused episode row (frame 2), after Mark finished and the
 * same size. Fed only what it shows, so it can move with the row (#205).
 */
@Composable
fun EpisodeFavouriteButton(filled: Boolean, playlist: String, onClick: () -> Unit, modifier: Modifier = Modifier) = RowButton(
    onClick = onClick,
    icon = heartIcon(filled),
    description = heartDescription(filled, playlist),
    modifier = modifier
)

private fun heartIcon(filled: Boolean) = if (filled) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder

/** "Favourite", or "Remove from <playlist>" once it is in. */
@Composable
private fun heartDescription(filled: Boolean, playlist: String): String =
    if (filled) stringResource(R.string.favourite_remove, playlist) else stringResource(R.string.favourite)
