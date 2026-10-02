package com.paulohenriquesg.fahrenheit.stats

import com.paulohenriquesg.fahrenheit.api.ItemStats
import com.paulohenriquesg.fahrenheit.api.LibraryItemMetadata
import com.paulohenriquesg.fahrenheit.api.ListeningSession
import com.paulohenriquesg.fahrenheit.api.ListeningStatsResponse
import org.junit.Assert.assertEquals
import org.junit.Test

class StatsSummaryTest {

    private fun metadata(title: String) = LibraryItemMetadata(
        title = title,
        titleIgnorePrefix = null,
        subtitle = null,
        publishedYear = null,
        publishedDate = null,
        publisher = null,
        description = null,
        isbn = null,
        asin = null,
        language = null,
        explicit = false
    )

    private fun stats(
        totalTime: Double = 0.0,
        today: Double = 0.0,
        items: Map<String, ItemStats> = emptyMap(),
        days: Map<String, Double> = emptyMap(),
        dayOfWeek: Map<String, Double> = emptyMap(),
        recentSessions: List<ListeningSession>? = null
    ) = ListeningStatsResponse(
        totalTime = totalTime,
        items = items,
        days = days,
        dayOfWeek = dayOfWeek,
        today = today,
        recentSessions = recentSessions
    )

    @Test
    fun `the headline figures come straight off the response`() {
        val summary = StatsSummary.of(
            stats(
                totalTime = 651_600.0,
                today = 1_440.0,
                items = mapOf("a" to ItemStats("a", 100.0), "b" to ItemStats("b", 50.0)),
                days = mapOf("2026-09-30" to 600.0, "2026-10-01" to 840.0)
            )
        )

        assertEquals(651_600.0, summary.totalListened, 0.001)
        assertEquals(1_440.0, summary.today, 0.001)
        assertEquals(2, summary.booksTouched)
        assertEquals(2, summary.activeDays)
        assertEquals(325_800.0, summary.averagePerDay, 0.001)
    }

    @Test
    fun `a day with no activity does not divide by zero`() {
        val summary = StatsSummary.of(stats(totalTime = 100.0, days = emptyMap()))

        assertEquals(0.0, summary.averagePerDay, 0.001)
    }

    @Test
    fun `the week is always seven days, Monday first, with the quiet ones at zero`() {
        val summary = StatsSummary.of(
            stats(dayOfWeek = mapOf("Monday" to 60.0, "Saturday" to 120.0))
        )

        assertEquals(
            listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"),
            summary.weekdays.map { it.label }
        )
        assertEquals(60.0, summary.weekdays.first().seconds, 0.001)
        assertEquals(0.0, summary.weekdays[1].seconds, 0.001)
        assertEquals(120.0, summary.weekdays[5].seconds, 0.001)
    }

    @Test
    fun `the server's spelling of a weekday does not have to match ours`() {
        val summary = StatsSummary.of(
            stats(dayOfWeek = mapOf("tuesday" to 30.0, "WED" to 45.0, "Caturday" to 999.0))
        )

        assertEquals(30.0, summary.weekdays[1].seconds, 0.001)
        assertEquals(45.0, summary.weekdays[2].seconds, 0.001)
        assertEquals(75.0, summary.weekdays.sumOf { it.seconds }, 0.001)
    }

    @Test
    fun `weekday bars are drawn against the busiest day`() {
        val summary = StatsSummary.of(
            stats(dayOfWeek = mapOf("Monday" to 50.0, "Friday" to 100.0))
        )

        assertEquals(0.5f, summary.weekdays[0].share, 0.001f)
        assertEquals(1.0f, summary.weekdays[4].share, 0.001f)
        assertEquals(0.0f, summary.weekdays[6].share, 0.001f)
    }

    @Test
    fun `a week with no listening has no bars rather than infinite ones`() {
        val summary = StatsSummary.of(stats(dayOfWeek = mapOf("Monday" to 0.0)))

        assertEquals(listOf(0.0f), summary.weekdays.map { it.share }.distinct())
    }

    @Test
    fun `the most listened books come first, titled, and only a few of them`() {
        val summary = StatsSummary.of(
            stats(
                items = mapOf(
                    "a" to ItemStats("a", 100.0, metadata("Middle")),
                    "b" to ItemStats("b", 300.0, metadata("Most")),
                    "c" to ItemStats("c", 10.0, metadata("Least")),
                    "d" to ItemStats("d", 200.0, metadata("Second"))
                )
            )
        )

        assertEquals(listOf("Most", "Second", "Middle"), summary.topBooks.map { it.title })
        assertEquals(1.0f, summary.topBooks[0].share, 0.001f)
        assertEquals(2f / 3f, summary.topBooks[1].share, 0.001f)
    }

    @Test
    fun `a book the server did not describe is still listed`() {
        val summary = StatsSummary.of(
            stats(items = mapOf("li_abc" to ItemStats("li_abc", 90.0, mediaMetadata = null)))
        )

        assertEquals(1, summary.topBooks.size)
        assertEquals("li_abc", summary.topBooks.single().title)
    }

    @Test
    fun `recent sessions are newest first, and only a few of them`() {
        val summary = StatsSummary.of(
            stats(
                recentSessions = listOf(
                    session("1", "Older", updatedAt = 1_000),
                    session("2", "Newest", updatedAt = 9_000),
                    session("3", "Middle", updatedAt = 5_000),
                    session("4", "Oldest", updatedAt = 10)
                )
            )
        )

        assertEquals(listOf("Newest", "Middle", "Older"), summary.recentSessions.map { it.title })
    }

    @Test
    fun `a session with no title falls back to something printable`() {
        val summary = StatsSummary.of(
            stats(recentSessions = listOf(session("1", null, updatedAt = 1)))
        )

        assertEquals("li_1", summary.recentSessions.single().title)
    }

    @Test
    fun `no sessions at all is not an error`() {
        val summary = StatsSummary.of(stats(recentSessions = null))

        assertEquals(emptyList<SessionRow>(), summary.recentSessions)
    }

    private fun session(id: String, title: String?, updatedAt: Long) = ListeningSession(
        id = id,
        userId = "u",
        libraryItemId = "li_$id",
        mediaType = "book",
        displayTitle = title,
        timeListening = 60.0,
        startTime = 0.0,
        currentTime = 60.0,
        startedAt = updatedAt,
        updatedAt = updatedAt
    )
}
