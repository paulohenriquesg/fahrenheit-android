package com.paulohenriquesg.fahrenheit.player

import com.paulohenriquesg.fahrenheit.api.MediaProgressResponse
import com.paulohenriquesg.fahrenheit.api.ProgressMark
import com.paulohenriquesg.fahrenheit.progress.ProgressStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * What the playback service puts on the server, it puts in the [store] too,
 * for every screen to show (#207).
 *
 * @param readBack an item's progress as the server now holds it; null or
 *   throwing when it cannot be read.
 */
class ProgressWrites(
    private val store: ProgressStore,
    private val readBack: suspend (itemId: String, episodeId: String?) -> MediaProgressResponse?
) {
    /** A report reached the server: where it is, against everything that will play. */
    fun delivered(file: QueuedFile, position: Double) =
        store.played(file.itemId, file.episodeId, position, file.bookTotal)

    /** A stretch closed: whether its end finished the item is the server's rule, a library setting. */
    suspend fun closed(file: QueuedFile) {
        val since = store.generation
        val progress = try {
            readBack(file.itemId, file.episodeId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
        // A stop is news for Home's shelves (#197), read back or not.
        store.stopped(file.itemId, file.episodeId, progress, since)
    }

    /** Sends a mark with [send], and stores it once the server took it. */
    suspend fun marked(itemId: String, episodeId: String?, mark: ProgressMark, send: suspend () -> Unit) {
        send()
        store.marked(itemId, episodeId, mark)
    }

    companion object {
        val process by lazy { ProgressWrites(ProgressStore.process, ResumeSources::progress) }

        // Outlives the service: the end of the queue ends it, and the read
        // that says the episode is finished comes after (#207).
        private val reading = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

        /** [closed], in a scope that outlives the playback service. */
        fun readBackLater(file: QueuedFile) {
            reading.launch { process.closed(file) }
        }
    }
}
