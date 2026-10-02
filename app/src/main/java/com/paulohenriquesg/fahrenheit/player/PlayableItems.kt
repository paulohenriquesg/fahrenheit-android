package com.paulohenriquesg.fahrenheit.player

import androidx.media3.common.MediaItem

/**
 * Turns items as a MediaController delivers them back into items a player can
 * play. Media3 strips an item's own URI on that trip; [PlaybackQueue] put a
 * copy in request metadata, which arrives intact.
 */
object PlayableItems {

    /** @return null if any item has no URI at all - a queue with a hole in it is refused whole. */
    fun resolve(items: List<MediaItem>): List<MediaItem>? = items.map { item ->
        when {
            item.localConfiguration != null -> item
            else -> {
                val uri = item.requestMetadata.mediaUri ?: return null
                item.buildUpon().setUri(uri).build()
            }
        }
    }
}
