package com.paulohenriquesg.fahrenheit.player

import android.content.Intent
import com.paulohenriquesg.fahrenheit.api.LibraryItem
import com.paulohenriquesg.fahrenheit.api.LibraryItemResponse
import com.paulohenriquesg.fahrenheit.ui.elements.LibraryItemDisplay
import com.paulohenriquesg.fahrenheit.ui.elements.coverUrlOf

/**
 * What the screen that opens the player already shows of the item, for the
 * player's loading frame (#223; frame 1 of docs/mocks/player-loading.html):
 * the title, the line above it (the author, or the podcast's name) and the
 * cover. Only for that frame: once the item has loaded, the player draws
 * what it loaded.
 *
 * @param kicker above the title: an episode's show. A book's is its series,
 *   which the opening screens do not show, so it has none here.
 * @param byline under the title: a book's author, where the player draws it.
 * @param coverUrl the cover the opening screen asked for; null when the item
 *   has none, and the frame draws the placeholder with [title] on it.
 */
data class PlayerPreview(
    val title: String,
    val kicker: String? = null,
    val coverUrl: String? = null,
    val byline: String? = null
) {

    internal fun writeTo(intent: Intent) {
        intent.putExtra(EXTRA_TITLE, title)
        kicker?.let { intent.putExtra(EXTRA_KICKER, it) }
        coverUrl?.let { intent.putExtra(EXTRA_COVER_URL, it) }
        byline?.let { intent.putExtra(EXTRA_BYLINE, it) }
    }

    companion object {
        private const val EXTRA_TITLE = "preview_title"
        private const val EXTRA_KICKER = "preview_kicker"
        private const val EXTRA_COVER_URL = "preview_cover_url"
        private const val EXTRA_BYLINE = "preview_byline"

        /** Null when the intent carries none, as from a screen that knew nothing. */
        internal fun from(intent: Intent): PlayerPreview? {
            val title = intent.getStringExtra(EXTRA_TITLE) ?: return null
            return PlayerPreview(
                title,
                intent.getStringExtra(EXTRA_KICKER),
                intent.getStringExtra(EXTRA_COVER_URL),
                intent.getStringExtra(EXTRA_BYLINE)
            )
        }

        /** A book: its author under the title; no cover asked for when it says it has none. */
        fun book(itemId: String, title: String?, author: String?, hasCover: Boolean = true): PlayerPreview? =
            of(title, null, if (hasCover) coverUrlOf(itemId) else null, author)

        /** An episode: its show above the title, and the show's cover, as the player draws it. */
        fun episode(podcastId: String, title: String?, show: String?, hasCover: Boolean = true): PlayerPreview? =
            of(title, show, if (hasCover) coverUrlOf(podcastId) else null)

        /**
         * A card on Home: a book's title and author, or the episode it stands
         * for with its show - what the card shows. No cover is asked for when
         * the server says there is none, as the card does not ask.
         */
        fun of(item: LibraryItem): PlayerPreview? {
            val hasCover = item.media.coverPath != null
            val episode = item.recentEpisode
            return if (item.mediaType == "podcast" && episode != null) {
                episode(episode.libraryItemId ?: item.id, LibraryItemDisplay.title(item), item.media.metadata.title, hasCover)
            } else {
                book(item.id, LibraryItemDisplay.title(item), LibraryItemDisplay.author(item), hasCover)
            }
        }

        /** A book's own screen: its title, and its authors as the screen lists them. */
        fun of(item: LibraryItemResponse): PlayerPreview? {
            val metadata = item.media.metadata
            val authors = metadata.authors.orEmpty().joinToString(", ") { it.name }.takeIf { it.isNotEmpty() }
            return book(item.id, metadata.title, authors ?: metadata.authorName)
        }

        /** Now playing, in the rail or the bar: the title it shows. */
        fun of(entry: RailEntry): PlayerPreview? = of(entry.title, null, coverUrlOf(entry.itemId))

        private fun of(title: String?, kicker: String?, coverUrl: String?, byline: String? = null): PlayerPreview? =
            title?.takeIf { it.isNotBlank() }?.let {
                PlayerPreview(it, kicker?.takeIf { k -> k.isNotBlank() }, coverUrl, byline?.takeIf { b -> b.isNotBlank() })
            }
    }
}
