package com.paulohenriquesg.fahrenheit.ui.elements

import android.content.Context
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.edit
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * The covers this device has drawn, newest first, so the login screen can
 * show the returning user's own covers before anyone is signed in (#161).
 *
 * Kept as the URLs [CoverImage] fetched them by - the key Coil caches them
 * under - in plain preferences: an address and item ids, nothing secret.
 */
object RecentCovers {
    const val MAX = 40

    private const val PREFS = "recent_covers"
    private const val KEY = "urls"
    // Drawn at a third of the screen's height at most: no need for the full cover.
    private const val SIDE_PX = 240

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun all(context: Context): List<String> =
        prefs(context).getString(KEY, null)?.split('\n')?.filter { it.isNotEmpty() }.orEmpty()

    /** [url] was just drawn: it goes to the front, and the oldest past [MAX] go. */
    fun note(context: Context, url: String) {
        val now = all(context)
        // A screen of covers drawn again writes nothing.
        if (now.firstOrNull() == url) return
        val next = (listOf(url) + now.filter { it != url }).take(MAX)
        prefs(context).edit { putString(KEY, next.joinToString("\n")) }
    }

    /** The covers drawn from [host], newest first. */
    fun forServer(context: Context, host: String): List<String> {
        val root = host.trimEnd('/')
        // An address kept with its trailing slash fetched covers as "//api".
        return all(context).filter { url ->
            url.startsWith("$root/") && url.substring(root.length).trimStart('/').startsWith("api/items/")
        }
    }

    /**
     * [host]'s covers that are on this device, decoded. Nothing is asked of
     * the network: a cover the cache no longer has is left out, not fetched.
     */
    suspend fun loadCached(context: Context, host: String): List<ImageBitmap> = coroutineScope {
        // All at once: one by one, forty disk reads held the wall back.
        forServer(context, host).map { url -> async { cached(context, url) } }.awaitAll().filterNotNull()
    }

    private suspend fun cached(context: Context, url: String): ImageBitmap? {
        val request = ImageRequest.Builder(context)
            .data(url)
            .networkCachePolicy(CachePolicy.DISABLED)
            // From the disk, as after a restart; not the copy just drawn.
            .memoryCachePolicy(CachePolicy.DISABLED)
            .size(SIDE_PX)
            .allowHardware(false)
            .build()
        return (context.imageLoader.execute(request) as? SuccessResult)
            ?.drawable?.toBitmap()?.asImageBitmap()
    }
}
