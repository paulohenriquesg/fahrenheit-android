package com.paulohenriquesg.fahrenheit.player

/** What the Sleep panel offers (#107). */
sealed interface SleepChoice {
    data object Off : SleepChoice
    data object EndOfChapter : SleepChoice
    data class Minutes(val minutes: Int) : SleepChoice

    companion object {
        val OFFERED: List<SleepChoice> = listOf(Off, EndOfChapter, Minutes(15), Minutes(30), Minutes(60))
    }
}

/**
 * When the sleep timer pauses playback, and how much it has left (#107).
 *
 * Minutes are minutes of listening: only what [listened] adds counts, so a
 * pause does not use the timer up. End of chapter follows the listener: it
 * waits for the end of the chapter playback is in, and a seek moves it to the
 * end of the chapter the seek landed in. Positions are whole-book seconds.
 */
class SleepTimer {
    var choice: SleepChoice = SleepChoice.Off
        private set

    private var listeningLeftMs = 0L
    private var chapterEnds = emptyList<Double>()
    private var target = 0.0

    fun minutes(minutes: Int) {
        choice = SleepChoice.Minutes(minutes)
        listeningLeftMs = minutes * 60_000L
    }

    fun endOfChapter(ends: List<Double>, position: Double) {
        choice = SleepChoice.EndOfChapter
        chapterEnds = ends.sorted()
        seeked(position)
    }

    fun off() {
        choice = SleepChoice.Off
    }

    /** Playback jumped: End of chapter now waits for the end of the chapter it landed in. */
    fun seeked(position: Double) {
        // A chapter's own start, just short after a seek, belongs to it.
        target = chapterEnds.firstOrNull { it > position + ChapterClock.AT_START } ?: chapterEnds.lastOrNull() ?: position
    }

    fun listened(ms: Long) {
        if (choice is SleepChoice.Minutes) listeningLeftMs -= ms
    }

    fun due(position: Double): Boolean = when (choice) {
        SleepChoice.Off -> false
        SleepChoice.EndOfChapter -> position >= target
        is SleepChoice.Minutes -> listeningLeftMs <= 0
    }

    /** Listening left before it pauses, at [speed]; null when it is off. */
    fun secondsLeft(position: Double, speed: Float): Double? = when (choice) {
        SleepChoice.Off -> null
        SleepChoice.EndOfChapter -> (target - position).coerceAtLeast(0.0) / speed
        is SleepChoice.Minutes -> listeningLeftMs.coerceAtLeast(0) / 1000.0
    }
}
