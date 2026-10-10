package com.paulohenriquesg.fahrenheit.progress

import com.paulohenriquesg.fahrenheit.api.LibraryItemResponse
import com.paulohenriquesg.fahrenheit.api.Me
import com.paulohenriquesg.fahrenheit.api.MediaProgressResponse
import com.paulohenriquesg.fahrenheit.api.ProgressMark
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** A book's progress, or one episode's: a podcast's episodes share its item id. */
data class ProgressKey(val itemId: String, val episodeId: String?)

/**
 * Listening progress for the whole app, which every screen reads (#207).
 *
 * The playback service writes what its reports and marks put on the server
 * ([played], [marked]); the server's own copy comes in whole ([replace], from
 * GET /api/me) or one item at a time ([read]). Reads are answered late, so
 * each says when it began ([generation]): what was written here after that
 * is newer than the answer, and stays.
 *
 * In memory, for the process; main thread.
 *
 * @param now this device's clock, for [MediaProgressResponse.lastUpdate] on
 *   what the player wrote. Resume compares it with the server's own stamps,
 *   so a write here is also stamped later than any of those.
 */
class ProgressStore(private val now: () -> Long = System::currentTimeMillis) {
    private val all = MutableStateFlow<Map<ProgressKey, MediaProgressResponse>>(emptyMap())

    val entries: StateFlow<Map<ProgressKey, MediaProgressResponse>> = all

    private val told = MutableStateFlow(0)

    /**
     * Counts the times listening started, stopped, or a mark changed whether
     * something is finished: what moves an item on or off Home's shelves
     * (#197). Not a resync, nor a page's own read.
     */
    val news: StateFlow<Int> = told

    /** Items whose stretch of listening is open: reported on since the last stop. */
    private val listening = mutableSetOf<ProgressKey>()

    /** Counts the writes made here; a read takes it as it begins. */
    var generation: Long = 0
        private set

    /** The generation of the last sign-out: a read begun before it is the last account's. */
    var clearedAt: Long = 0
        private set

    /** The generation each key was last written here at. */
    private val written = mutableMapOf<ProgressKey, Long>()

    fun of(itemId: String, episodeId: String? = null): MediaProgressResponse? = all.value[ProgressKey(itemId, episodeId)]

    /** Everything the server holds, read since [since]: it replaces all but what was written here after. */
    fun replace(progress: List<MediaProgressResponse>, since: Long) {
        if (since < clearedAt) return
        val kept = all.value.filterKeys { newerThan(it, since) }
        val read = progress.mapNotNull { p -> keyOf(p)?.let { it to p } }.toMap().filterKeys { !newerThan(it, since) }
        written.keys.retainAll(kept.keys)
        all.value = read + kept
    }

    /** One item as the server holds it, read since [since]. */
    fun read(progress: MediaProgressResponse, since: Long) {
        val key = keyOf(progress) ?: return
        if (since < clearedAt || newerThan(key, since)) return
        all.value = all.value + (key to progress)
    }

    /** An item read with its own progress (a book's page); one never started adds nothing. */
    fun readItem(item: LibraryItemResponse, since: Long) {
        val progress = item.userMediaProgress ?: return
        read(progress.copy(libraryItemId = progress.libraryItemId ?: item.id), since)
    }

    /**
     * A report of the player's reached the server. The server un-finishes an
     * item whose position moves; whether the end finishes one is its rule,
     * read back after the stop.
     */
    fun played(itemId: String, episodeId: String?, position: Double, duration: Double?) {
        // The server counts it started from the first report it took.
        if (listening.add(ProgressKey(itemId, episodeId))) told.value++
        playedAt(itemId, episodeId, position, duration)
    }

    /**
     * A stretch of listening closed, and the item as the server now holds it,
     * read since [since] - whether the end finished it is its rule; null when
     * that read failed.
     */
    fun stopped(itemId: String, episodeId: String?, progress: MediaProgressResponse?, since: Long) {
        listening.remove(ProgressKey(itemId, episodeId))
        progress?.let { read(it, since) }
        told.value++
    }

    private fun playedAt(itemId: String, episodeId: String?, position: Double, duration: Double?) = write(itemId, episodeId) { old ->
        val length = duration ?: old?.duration
        (old ?: MediaProgressResponse(libraryItemId = itemId, episodeId = episodeId)).copy(
            currentTime = position,
            duration = length,
            progress = length?.takeIf { it > 0 }?.let { (position / it).coerceIn(0.0, 1.0) } ?: old?.progress,
            isFinished = if (old?.currentTime == position) old.isFinished ?: false else false
        )
    }

    /**
     * Mark finished or unfinished, or a position, reached the server. As
     * there, finishing puts progress at the whole, and un-finishing something
     * finished puts it back at the start.
     */
    fun marked(itemId: String, episodeId: String?, mark: ProgressMark) {
        val was = of(itemId, episodeId)?.isFinished == true
        markedAt(itemId, episodeId, mark)
        if (of(itemId, episodeId)?.isFinished == true != was) told.value++
    }

    private fun markedAt(itemId: String, episodeId: String?, mark: ProgressMark) = write(itemId, episodeId) { old ->
        var next = old ?: MediaProgressResponse(libraryItemId = itemId, episodeId = episodeId, currentTime = 0.0)
        mark.isFinished?.let { finished ->
            next = when {
                finished -> next.copy(isFinished = true, progress = 1.0)
                next.isFinished == true -> next.copy(isFinished = false, currentTime = 0.0, progress = 0.0)
                else -> next.copy(isFinished = false)
            }
        }
        mark.currentTime?.let { at ->
            next = next.copy(currentTime = at, progress = next.duration?.takeIf { it > 0 }?.let { (at / it).coerceIn(0.0, 1.0) })
        }
        next
    }

    /** A reply from GET /api/me, read since [since]; one without the list says nothing of progress. */
    fun readMe(me: Me, since: Long) {
        me.mediaProgress?.let { replace(it, since) }
    }

    /** Signed out: none of it is the next user's. */
    fun clear() {
        clearedAt = ++generation
        written.clear()
        listening.clear()
        all.value = emptyMap()
    }

    private fun write(itemId: String, episodeId: String?, change: (MediaProgressResponse?) -> MediaProgressResponse) {
        val key = ProgressKey(itemId, episodeId)
        written[key] = ++generation
        // Later than anything known, whatever the two clocks say.
        val latest = all.value.values.maxOfOrNull { it.lastUpdate ?: 0L } ?: 0L
        all.value = all.value + (key to change(all.value[key]).copy(lastUpdate = maxOf(now(), latest + 1)))
    }

    private fun newerThan(key: ProgressKey, since: Long) = (written[key] ?: 0) > since

    private fun keyOf(p: MediaProgressResponse) = p.libraryItemId?.let { ProgressKey(it, p.episodeId) }

    companion object {
        /** The playback service and every screen share one. */
        val process = ProgressStore()
    }
}
