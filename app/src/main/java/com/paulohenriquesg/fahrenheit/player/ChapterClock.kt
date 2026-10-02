package com.paulohenriquesg.fahrenheit.player

import com.paulohenriquesg.fahrenheit.api.Chapter

/** One chapter on the book's timeline, in whole-book seconds. */
data class ChapterSpan(val index: Int, val title: String, val start: Double, val end: Double) {
    fun elapsed(at: Double): Double = (at - start).coerceIn(0.0, end - start)
    fun left(at: Double): Double = (end - at).coerceIn(0.0, end - start)
    fun fraction(at: Double): Float = if (end <= start) 0f else ((at - start) / (end - start)).coerceIn(0.0, 1.0).toFloat()
}

/**
 * Where a position sits in its chapter, and where chapter skip goes (#107).
 *
 * Chapters come as the server stores them: sorted here, and a missing end
 * runs to the next chapter's start, or the book's end for the last one.
 */
object ChapterClock {
    /** Previous within this many seconds of a chapter's start goes to the one before; later, it restarts this one. */
    const val RESTART_WITHIN = 3.0

    fun spans(chapters: List<Chapter>?, total: Double): List<ChapterSpan> {
        val sorted = chapters.orEmpty().filter { it.start != null }.sortedBy { it.start }
        return sorted.mapIndexedNotNull { i, c ->
            val start = c.start!!
            val end = c.end ?: sorted.getOrNull(i + 1)?.start ?: total
            if (end <= start) null else ChapterSpan(i, c.title.orEmpty(), start, end)
        }
    }

    fun at(spans: List<ChapterSpan>, at: Double): ChapterSpan? =
        spans.lastOrNull { it.start <= at } ?: spans.firstOrNull()

    fun previousTarget(spans: List<ChapterSpan>, at: Double): Double? {
        val current = at(spans, at) ?: return null
        if (at - current.start > RESTART_WITHIN) return current.start
        val i = spans.indexOf(current)
        return if (i > 0) spans[i - 1].start else current.start
    }

    fun nextTarget(spans: List<ChapterSpan>, at: Double): Double? {
        val current = at(spans, at) ?: return null
        return spans.getOrNull(spans.indexOf(current) + 1)?.start
    }
}
