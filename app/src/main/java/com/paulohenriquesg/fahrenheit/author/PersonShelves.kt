package com.paulohenriquesg.fahrenheit.author

import com.paulohenriquesg.fahrenheit.api.AuthorDetailResponse
import com.paulohenriquesg.fahrenheit.api.LibraryItem
import com.paulohenriquesg.fahrenheit.api.MediaProgressResponse
import com.paulohenriquesg.fahrenheit.api.Series
import com.paulohenriquesg.fahrenheit.progress.ProgressKey
import com.paulohenriquesg.fahrenheit.utils.Alphabetical
import com.paulohenriquesg.fahrenheit.utils.listeningLength

/** A book on a shelf of the author's or narrator's page, with its place in that shelf's series. */
data class ShelfBook(val item: LibraryItem, val number: String?)

/** One row of the page: a series' books, or [series] null for the books in none ("Other books"). */
data class PersonShelf(val series: String?, val books: List<ShelfBook>)

/**
 * An author's or a narrator's books as their page lays them out (#230): one
 * shelf per series, by name, its books in series order, then the books in no
 * series by title. A book in two series is on both shelves.
 */
object PersonShelves {

    /** Books that carry their series with each one's sequence, as an expanded items list does. */
    fun of(items: List<LibraryItem>): List<PersonShelf> = of(items) { it.media.metadata.series.orEmpty() }

    /**
     * An author's books: the item list carries no sequence, so each book's
     * places come from the author's series, each listing its books with theirs.
     */
    fun of(author: AuthorDetailResponse): List<PersonShelf> {
        val places = author.series.orEmpty().flatMap { group ->
            group.items.orEmpty().map { item ->
                item.id to Series(id = group.id, name = group.name, sequence = item.media.metadata.series?.firstOrNull()?.sequence)
            }
        }.groupBy({ it.first }, { it.second })
        return of(author.libraryItems.orEmpty()) { places[it.id].orEmpty() }
    }

    private fun of(items: List<LibraryItem>, seriesOf: (LibraryItem) -> List<Series>): List<PersonShelf> {
        val byTitle = Alphabetical.byName<ShelfBook> { it.item.media.metadata.title }
        val inSeries = items
            .flatMap { item -> seriesOf(item).map { it to ShelfBook(item, it.sequence?.takeIf(String::isNotBlank)) } }
            .groupBy({ it.first.id }, { it })
            .map { (_, entries) ->
                PersonShelf(entries.first().first.name, entries.map { it.second }.sortedWith(bySequence.then(byTitle)))
            }
            .sortedWith(Alphabetical.byName { it.series })
        val others = items
            .filter { seriesOf(it).isEmpty() }
            .map { ShelfBook(it, null) }
            .sortedWith(byTitle)
        return if (others.isEmpty()) inSeries else inSeries + PersonShelf(null, others)
    }

    /** Numbered books first, by number; a sequence that is not a number after those that are. */
    private val bySequence: Comparator<ShelfBook> =
        compareBy<ShelfBook, Double?>(nullsLast()) { it.number?.toDoubleOrNull() }
            .thenBy(nullsLast()) { it.number?.takeIf { n -> n.toDoubleOrNull() == null } }

    /**
     * The book Continue plays: of [items], the one last played among those
     * started and not finished, as the shared store (#207) holds them.
     */
    fun continueOf(items: List<LibraryItem>, progress: Map<ProgressKey, MediaProgressResponse>): LibraryItem? =
        items
            .mapNotNull { item -> progress[ProgressKey(item.id, null)]?.let { item to it } }
            .filter { (_, p) -> p.isFinished != true && (p.currentTime ?: 0.0) > 0 }
            .maxByOrNull { (_, p) -> p.lastUpdate ?: 0 }
            ?.first
}

/**
 * What a card says to tell its book apart (#230): the subtitle as a [tag]
 * ("Unabridged"), and under the title its number in the series and its
 * length - or, on a narrator's page ([byAuthor]), who wrote it.
 */
data class PersonCard(val tag: String?, val detail: String) {
    companion object {
        fun of(book: ShelfBook, byAuthor: Boolean): PersonCard {
            val metadata = book.item.media.metadata
            val author = metadata.authorName?.takeIf { it.isNotBlank() }
                ?: metadata.authors.orEmpty().joinToString(", ") { it.name }.takeIf { it.isNotBlank() }
            val second = if (byAuthor && author != null) author else listeningLength(book.item.media.duration)
            return PersonCard(
                tag = metadata.subtitle?.takeIf { it.isNotBlank() },
                detail = listOfNotNull(book.number?.let { "#$it" }, second).joinToString(" · ")
            )
        }
    }
}
