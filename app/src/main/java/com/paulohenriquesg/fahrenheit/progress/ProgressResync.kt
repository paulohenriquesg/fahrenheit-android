package com.paulohenriquesg.fahrenheit.progress

import com.paulohenriquesg.fahrenheit.api.ApiClient
import com.paulohenriquesg.fahrenheit.api.MediaProgressResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Reads the server's progress into the [store] (#207): listening done
 * elsewhere - another device, the web - shows on coming back to the app. One
 * read at a time; one that fails leaves the store as it is.
 *
 * @param fetch everything the user has started; null when signed out.
 */
class ProgressResync(
    private val store: ProgressStore,
    private val scope: CoroutineScope,
    private val fetch: suspend () -> List<MediaProgressResponse>?
) {
    private var reading: Job? = null

    /** The sign-in ([ProgressStore.clearedAt]) the read out is for. */
    private var readingFor = -1L

    /** The sign-in a read last succeeded for. */
    private var readFor = -1L

    fun request() {
        // One begun for the last account does not hold up the next.
        if (reading?.isActive == true && readingFor == store.clearedAt) return
        val signIn = store.clearedAt
        readingFor = signIn
        reading = scope.launch {
            val since = store.generation
            val progress = runCatching { fetch() }.getOrNull() ?: return@launch
            store.replace(progress, since)
            readFor = signIn
        }
    }

    /**
     * Reads only if nothing has been read since signing in: a TV launched
     * before its network was up is rarely sent to the background, which is
     * when [request] is next asked.
     */
    fun requestIfNeverRead() {
        if (readFor != store.clearedAt) request()
    }

    companion object {
        /** Asked by the Application on coming to the foreground, and by Home ([requestIfNeverRead] on each return). */
        val process by lazy {
            ProgressResync(ProgressStore.process, CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)) {
                // A reply without the list says nothing about it, rather than "nothing started".
                ApiClient.getPodcastApi()?.me()?.mediaProgress
            }
        }
    }
}
