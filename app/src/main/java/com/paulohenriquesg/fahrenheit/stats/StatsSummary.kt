package com.paulohenriquesg.fahrenheit.stats

import com.paulohenriquesg.fahrenheit.api.ListeningStatsResponse
import java.util.Locale

/** A day of the week with what was listened on it, sized against the busiest day. */
data class WeekdayShare(val label: String, val seconds: Double, val share: Float)

/** A book and the time spent in it, sized against the most listened one. */
data class BookTime(val id: String, val title: String, val seconds: Double, val share: Float)

/** One listening session, as a row. [libraryItemId] is where its cover comes from. */
data class SessionRow(
    val id: String,
    val libraryItemId: String,
    val title: String,
    val seconds: Double,
    val updatedAt: Long
)

/**
 * Everything the stats screen draws, worked out from one response.
 *
 * The response carries far more than the four numbers the screen used to show:
 * per-weekday totals, per-book time and recent sessions are all in there.
 */
data class StatsSummary(
    val totalListened: Double,
    val today: Double,
    val booksTouched: Int,
    val activeDays: Int,
    val averagePerDay: Double,
    val weekdays: List<WeekdayShare>,
    val topBooks: List<BookTime>,
    val recentSessions: List<SessionRow>
) {
    companion object {
        const val TOP_BOOKS = 3
        const val RECENT_SESSIONS = 3

        // The server names the days; we label them short, and start the week on
        // Monday because a listening week reads better with the weekend last.
        private val WEEK = listOf(
            "Mon" to "monday",
            "Tue" to "tuesday",
            "Wed" to "wednesday",
            "Thu" to "thursday",
            "Fri" to "friday",
            "Sat" to "saturday",
            "Sun" to "sunday"
        )

        fun of(stats: ListeningStatsResponse): StatsSummary {
            val activeDays = stats.days.size
            return StatsSummary(
                totalListened = stats.totalTime,
                today = stats.today,
                booksTouched = stats.items.size,
                activeDays = activeDays,
                averagePerDay = if (activeDays > 0) stats.totalTime / activeDays else 0.0,
                weekdays = weekdaysOf(stats.dayOfWeek),
                topBooks = topBooksOf(stats),
                recentSessions = sessionsOf(stats)
            )
        }

        private fun weekdaysOf(dayOfWeek: Map<String, Double>): List<WeekdayShare> {
            val seconds = DoubleArray(WEEK.size)
            dayOfWeek.forEach { (key, value) ->
                // Accept whatever spelling arrives: "Monday", "monday", "MON".
                val named = key.trim().lowercase(Locale.ROOT)
                val day = WEEK.indexOfFirst { (_, full) ->
                    named == full || (named.length == 3 && full.startsWith(named))
                }
                if (day >= 0) seconds[day] += value
            }
            val busiest = seconds.max()
            return WEEK.mapIndexed { index, (label, _) ->
                WeekdayShare(
                    label = label,
                    seconds = seconds[index],
                    share = if (busiest > 0) (seconds[index] / busiest).toFloat() else 0f
                )
            }
        }

        private fun topBooksOf(stats: ListeningStatsResponse): List<BookTime> {
            val top = stats.items.values
                .sortedByDescending { it.timeListening }
                .take(TOP_BOOKS)
            val most = top.firstOrNull()?.timeListening ?: 0.0
            return top.map { item ->
                BookTime(
                    id = item.id,
                    // A book the server did not describe is still worth listing.
                    title = item.mediaMetadata?.title ?: item.id,
                    seconds = item.timeListening,
                    share = if (most > 0) (item.timeListening / most).toFloat() else 0f
                )
            }
        }

        /**
         * The latest books, not the latest sessions: the server returns one
         * session per sitting, so picking a book up twice would otherwise fill
         * the row with the same cover.
         */
        private fun sessionsOf(stats: ListeningStatsResponse): List<SessionRow> =
            stats.recentSessions.orEmpty()
                .groupBy { it.libraryItemId }
                .map { (libraryItemId, sittings) ->
                    val latest = sittings.maxBy { it.updatedAt }
                    SessionRow(
                        id = latest.id,
                        libraryItemId = libraryItemId,
                        title = latest.displayTitle ?: libraryItemId,
                        seconds = sittings.sumOf { it.timeListening },
                        updatedAt = latest.updatedAt
                    )
                }
                .sortedByDescending { it.updatedAt }
                .take(RECENT_SESSIONS)
    }
}
