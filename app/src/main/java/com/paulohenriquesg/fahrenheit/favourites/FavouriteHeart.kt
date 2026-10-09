package com.paulohenriquesg.fahrenheit.favourites

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

const val FAVOURITE_BUTTON_TAG = "player_favourite"

/** What a press on the heart did, for the short note that confirms it. */
sealed interface HeartChange {
    data class Added(val playlist: String) : HeartChange
    data class Removed(val playlist: String) : HeartChange
    data object Failed : HeartChange
}

/**
 * The heart for one book or episode (#180): there only while the library has
 * a Favourites playlist, filled while this is in it. A press that fails leaves
 * it as it was.
 */
@Stable
class FavouriteHeart(
    private val favourites: Favourites,
    private val libraryId: String,
    private val itemId: String,
    private val episodeId: String?
) {
    /** The library's Favourites playlist; null for None, or not read yet. */
    var playlist by mutableStateOf<Playlist?>(null)
        private set

    val filled: Boolean get() = playlist?.holds(itemId, episodeId) == true

    private var busy = false

    suspend fun load() {
        playlist = favourites.current(libraryId).getOrNull()
    }

    /** Adds or takes out; on a failure the heart shows the playlist as the server now has it, or as it was offline. 
     * Null null for a press while the last is still on its way. */
    suspend fun toggle(): HeartChange? {
        val before = playlist ?: return null
        if (busy) return null
        busy = true
        val wasIn = filled
        return try {
            favourites.toggle(before, itemId, episodeId).fold(
                onSuccess = { after ->
                    playlist = after
                    if (wasIn) HeartChange.Removed(after.name) else HeartChange.Added(after.name)
                },
                onFailure = {
                    // Read it again: offline it stays as it was, but a playlist
                    // that has gone (deleted elsewhere, or with its last item and
                    // not made again) takes the heart away rather than failing forever.
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
        Icon(
            if (filled) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            contentDescription = if (filled) stringResource(R.string.favourite_remove, playlist) else stringResource(R.string.favourite)
        )
    }
}
