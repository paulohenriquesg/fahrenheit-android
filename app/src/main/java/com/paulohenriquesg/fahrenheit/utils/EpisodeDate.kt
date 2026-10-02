package com.paulohenriquesg.fahrenheit.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * When an episode came out, said the way someone asking "is this new?" would
 * want to hear it.
 *
 * The latest-episodes list showed no date at all, so a brand new episode looked
 * exactly like one from March.
 */
object EpisodeDate {

    private const val DAY = 24 * 60 * 60 * 1000L
    private const val FALLBACK = "dd/MM/yyyy"

    /**
     * @param serverFormat how the server writes dates, from the login response.
     *   Anything Java cannot use falls back to day-first.
     */
    fun of(publishedAt: Long?, now: Long, serverFormat: String? = null): String {
        if (publishedAt == null || publishedAt <= 0) return ""
        val age = now - publishedAt
        return when {
            // Feeds do publish ahead of time; "in 2 days ago" is nonsense.
            age < DAY -> "Today"
            age < 2 * DAY -> "Yesterday"
            age < 7 * DAY -> "${age / DAY} days ago"
            else -> {
                val pattern = ServerDateFormat.pattern(serverFormat) ?: FALLBACK
                SimpleDateFormat(pattern, Locale.getDefault()).format(Date(publishedAt))
            }
        }
    }
}
