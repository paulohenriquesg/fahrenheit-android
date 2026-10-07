package com.paulohenriquesg.fahrenheit.player

import com.google.gson.Gson
import com.paulohenriquesg.fahrenheit.api.LibraryItemMetadata
import org.junit.Assert.assertEquals
import org.junit.Test

/** About's facts, one line each, the missing ones left out (#107). */
class AboutFactsTest {
    private fun metadata(json: String): LibraryItemMetadata =
        Gson().fromJson("""{"title":"A Book","explicit":false$json}""", LibraryItemMetadata::class.java)

    @Test fun `a book's facts in the mock's order`() = assertEquals(
        listOf(
            AboutFact(AboutFact.Kind.ReadBy, "A Reader, Another Reader"),
            AboutFact(AboutFact.Kind.Publisher, "A Press"),
            AboutFact(AboutFact.Kind.Published, "2015"),
            AboutFact(AboutFact.Kind.Length, "1 h 30 min"),
            AboutFact(AboutFact.Kind.Genres, "Science fiction, Thriller")
        ),
        AboutFacts.book(
            metadata(""","narrators":["A Reader","Another Reader"],"publisher":"A Press","publishedYear":"2015","genres":["Science fiction","Thriller"]"""),
            length = 5400.0
        )
    )

    @Test fun `missing facts are left out`() = assertEquals(
        listOf(AboutFact(AboutFact.Kind.Length, "10 min 0 s")),
        AboutFacts.book(metadata(""","publisher":" ","genres":[]"""), length = 600.0)
    )

    @Test fun `a minified narrator name is used when there is no list`() = assertEquals(
        AboutFact(AboutFact.Kind.ReadBy, "A Reader"),
        AboutFacts.book(metadata(""","narratorName":"A Reader""""), length = null).single()
    )

    // #178: the show, then when it came out and how long it is, then its number when the feed gives one.
    @Test fun `an episode's facts are its show, date, length, season and number`() = assertEquals(
        listOf(
            AboutFact(AboutFact.Kind.Show, "A Show"),
            AboutFact(AboutFact.Kind.Published, "Yesterday"),
            AboutFact(AboutFact.Kind.Length, "30 min 0 s"),
            AboutFact(AboutFact.Kind.Season, "2"),
            AboutFact(AboutFact.Kind.Episode, "14")
        ),
        AboutFacts.episode(show = "A Show", published = "Yesterday", length = 1800.0, season = " 2 ", episode = "14")
    )

    @Test fun `an episode's missing facts are left out`() = assertEquals(
        listOf(AboutFact(AboutFact.Kind.Length, "30 min 0 s")),
        AboutFacts.episode(show = " ", published = "", length = 1800.0, season = null, episode = " ")
    )
}
