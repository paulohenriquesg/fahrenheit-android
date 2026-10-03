package com.paulohenriquesg.fahrenheit.player

import android.os.Bundle
import androidx.media3.common.MediaItem

/**
 * What one queued file knows about the book it belongs to.
 *
 * Carried in the MediaItem's request metadata because that is what survives
 * the trip from the screen's controller to the service: Media3 strips the
 * playable URI on the way, but keeps request metadata. The service reports
 * progress from these facts alone, so it never needs to know what a book is.
 *
 * @property startOffset seconds from the start of the book where this file begins.
 * @property bookTotal the length of everything that will play, in seconds.
 * @property startAt where to start when playback moves on to this item by
 *   itself (#108: the next episode, from where it was left); 0 for the start.
 */
data class QueuedFile(
    val itemId: String,
    val episodeId: String?,
    val startOffset: Double,
    val bookTotal: Double,
    val startAt: Double = 0.0
) {
    /** Whole-book time for a position within this file - what progress sync reports. */
    fun bookTime(positionInFile: Double): Double = startOffset + positionInFile

    /** Whether this file belongs to that book, or to that episode of that podcast. */
    fun isFor(itemId: String, episodeId: String?): Boolean =
        this.itemId == itemId && this.episodeId == episodeId

    fun toBundle(): Bundle = Bundle().apply {
        putString(ITEM_ID, itemId)
        episodeId?.let { putString(EPISODE_ID, it) }
        putDouble(START_OFFSET, startOffset)
        putDouble(BOOK_TOTAL, bookTotal)
        putDouble(START_AT, startAt)
    }

    companion object {
        private const val ITEM_ID = "fahrenheit.itemId"
        private const val EPISODE_ID = "fahrenheit.episodeId"
        private const val START_OFFSET = "fahrenheit.startOffset"
        private const val BOOK_TOTAL = "fahrenheit.bookTotal"
        private const val START_AT = "fahrenheit.startAt"

        /** The facts an item carries, or null for an item that is not one of ours. */
        fun of(item: MediaItem?): QueuedFile? {
            val extras = item?.requestMetadata?.extras ?: return null
            val itemId = extras.getString(ITEM_ID) ?: return null
            return QueuedFile(
                itemId = itemId,
                episodeId = extras.getString(EPISODE_ID),
                startOffset = extras.getDouble(START_OFFSET),
                bookTotal = extras.getDouble(BOOK_TOTAL),
                startAt = extras.getDouble(START_AT)
            )
        }
    }
}
