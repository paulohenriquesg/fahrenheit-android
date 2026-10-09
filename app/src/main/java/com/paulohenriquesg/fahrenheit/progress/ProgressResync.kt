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

    fun request() {
        if (reading?.isActive == true) return
        reading = scope.launch {
            val since = store.generation
            val progress = runCatching { fetch() }.getOrNull() ?: return@launch
            store.replace(progress, since)
        }
    }

    companion object {
        /** Asked by the Application on coming to the foreground, and by Home. */
        val process by lazy {
            ProgressResync(ProgressStore.process, CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)) {
                // A reply without the list says nothing about it, rather than "nothing started".
                ApiClient.getPodcastApi()?.me()?.mediaProgress
            }
        }
    }
}
