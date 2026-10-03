package com.paulohenriquesg.fahrenheit.player

import com.paulohenriquesg.fahrenheit.api.Chapter

/** One chapter on the book's timeline, in whole-book seconds. */
data class ChapterSpan(val title: String, val start: Double, val end: Double) {
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

    /**
     * A seek keeps whole milliseconds and chapter starts are fractional, so a
     * position just short of a start - read straight after skipping to it -
     * belongs to that chapter, not the one before.
     */
    internal const val AT_START = 0.05

    fun spans(chapters: List<Chapter>?, total: Double): List<ChapterSpan> {
        val sorted = chapters.orEmpty().filter { it.start != null }.sortedBy { it.start }
        return sorted.mapIndexedNotNull { i, c ->
            val start = c.start!!
            val end = c.end ?: sorted.getOrNull(i + 1)?.start ?: total
            if (end <= start) null else ChapterSpan(c.title.orEmpty(), start, end)
        }
    }

    fun at(spans: List<ChapterSpan>, at: Double): ChapterSpan? =
        spans.lastOrNull { it.start <= at + AT_START } ?: spans.firstOrNull()

    fun previousTarget(spans: List<ChapterSpan>, at: Double): Double? {
        val current = at(spans, at) ?: return null
        // Before the first chapter (one that starts after 0), back is the start.
        if (at + AT_START < current.start) return 0.0
        if (at - current.start > RESTART_WITHIN) return current.start
        val i = spans.indexOf(current)
        return if (i > 0) spans[i - 1].start else current.start
    }

    fun nextTarget(spans: List<ChapterSpan>, at: Double): Double? {
        val current = at(spans, at) ?: return null
        if (at + AT_START < current.start) return current.start
        return spans.getOrNull(spans.indexOf(current) + 1)?.start
    }

    /** Where each chapter after the first begins, as fractions of the book: the book bar's ticks. */
    fun ticks(spans: List<ChapterSpan>, total: Double): List<Float> =
        if (total <= 0) emptyList() else spans.drop(1).map { (it.start / total).toFloat() }

    /**
     * Ticks a bar [widthPx] wide can show apart (#143): the first, then each
     * at least [minGapPx] from the last one kept. A book of a hundred short
     * chapters otherwise draws a comb; a few chapters keep every tick.
     */
    fun thinned(ticks: List<Float>, widthPx: Float, minGapPx: Float): List<Float> {
        val kept = mutableListOf<Float>()
        for (t in ticks) {
            val last = kept.lastOrNull()
            if (last == null || (t - last) * widthPx >= minGapPx) kept += t
        }
        return kept
    }
}
