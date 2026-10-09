package com.paulohenriquesg.fahrenheit.player

import com.paulohenriquesg.fahrenheit.api.LibraryItem

/** @param sequence its place in the series as the server gives it: "2", "2.5". */
data class SeriesBook(val itemId: String, val title: String, val sequence: String? = null) {
    /** Its place, for "Book 2.5" under its cover; null when it has none (#194). */
    val number: String? get() = sequence?.trim()?.takeIf { it.isNotEmpty() }
}

/**
 * The books of a series in series order, and where the one playing sits
 * among them (#107: About's series row, and "Book N of M").
 */
data class SeriesBooks(val books: List<SeriesBook>, val currentId: String) {
    /** This book's place in [books]; null when the list does not have it. */
    val current: Int? get() = books.indexOfFirst { it.itemId == currentId }.takeIf { it >= 0 }
    val total: Int get() = books.size

    companion object {
        fun of(items: List<LibraryItem>, currentId: String) =
            SeriesBooks(
                items.map { SeriesBook(it.id, it.media.metadata.title, it.media.metadata.series?.firstOrNull()?.sequence) },
                currentId
            )
    }
}
