package com.paulohenriquesg.fahrenheit.screensaver

import android.app.Activity
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.media3.session.MediaController
import com.paulohenriquesg.fahrenheit.R
import com.paulohenriquesg.fahrenheit.api.ApiClient
import android.content.Context
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.paulohenriquesg.fahrenheit.ui.elements.RecentCovers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import com.paulohenriquesg.fahrenheit.api.LibraryRepository
import com.paulohenriquesg.fahrenheit.login.LoginActivity
import com.paulohenriquesg.fahrenheit.player.Playback
import com.paulohenriquesg.fahrenheit.player.PlayerSettings
import com.paulohenriquesg.fahrenheit.player.RailEntry
import com.paulohenriquesg.fahrenheit.player.coverBitmap
import com.paulohenriquesg.fahrenheit.player.coverWashOf
import com.paulohenriquesg.fahrenheit.player.rememberRailEntry
import com.paulohenriquesg.fahrenheit.utils.listeningLength

/**
 * Puts the listening screensaver (#156) on a screen of the app, once: called
 * as each screen starts, after its own content is set, so the overlay sits
 * on top. The sign-in screen has nothing playing and is left alone.
 */
fun installListeningScreensaver(activity: Activity) {
    if (activity !is ComponentActivity || activity is LoginActivity) return
    if (activity.window.decorView.findViewWithTag<android.view.View>(ScreensaverTags.OVERLAY) != null) return
    val settings = PlayerSettings(activity)
    Screensaver.install(
        activity,
        listening = { rememberListening() },
        settings = { ScreensaverSettings(settings.screensaverMinutes, settings.screensaverStyle) },
        clock = SystemClock::uptimeMillis
    )
}

/**
 * What the playback service has queued, from a controller of this screen's
 * own: whether it plays, the now-playing line as the rail's entry reads it,
 * the covers for the wall and the playing cover's colour. Null when nothing
 * is queued.
 */
@Composable
private fun rememberListening(): Listening? {
    val context = LocalContext.current
    var controller by remember { mutableStateOf<MediaController?>(null) }
    DisposableEffect(Unit) {
        val future = Playback.connect(context)
        future.addListener({ controller = runCatching { future.get() }.getOrNull() }, ContextCompat.getMainExecutor(context))
        onDispose { MediaController.releaseFuture(future) }
    }
    val entry = rememberRailEntry(controller) { itemId ->
        ApiClient.getLibraryApi()?.let { LibraryRepository(it).item(itemId).getOrNull() }?.media?.chapters
    } ?: return null
    val covers by produceState(emptyList<ImageBitmap>(), entry.itemId) { value = wallCovers(context, entry.itemId) }
    val wash by produceState<Color?>(null, entry.itemId) { value = coverWashOf(coverBitmap(context, entry.itemId)) }
    return Listening(entry.playing, NowPlayingLine(entry.itemId, entry.title, detail(entry)), covers, wash)
}

/** "Chapter 12 · 18 min left in chapter"; "1 h 5 min left" without chapters; nothing for an episode. */
@Composable
private fun detail(entry: RailEntry): String? {
    if (entry.episodeId != null) return null
    val left = listeningLength(entry.leftSeconds)
    val chapter = entry.chapter ?: entry.chapterNumber?.let { stringResource(R.string.chapter_number, it) }
    return chapter?.let { stringResource(R.string.screensaver_chapter_left, it, left) }
        ?: stringResource(R.string.time_left, left)
}

/**
 * The wall's covers: those of the series being played, then the covers of
 * this server this device has drawn lately (#168's [RecentCovers]), each once.
 * Nothing on any failure: the wall is then the wash alone.
 */
private suspend fun wallCovers(context: Context, itemId: String): List<ImageBitmap> {
    val host = ApiClient.generateFullUrl("") ?: return emptyList()
    val repository = ApiClient.getLibraryApi()?.let { LibraryRepository(it) }
    val item = repository?.item(itemId)?.getOrNull()
    val series = item?.media?.metadata?.series?.firstOrNull()?.id
        ?.let { repository.seriesBooks(item.libraryId, it).getOrNull() }.orEmpty()
        .mapNotNull { book -> ApiClient.generateFullUrl("/api/items/${book.id}/cover") }
    val recent = RecentCovers.forServer(context, host)
    return coroutineScope {
        WallCovers.pick(series, recent) { it }.take(WALL_COVERS)
            .map { url -> async { coverAt(context, url) } }
            .awaitAll()
            .filterNotNull()
    }
}

/** One cover, small, from the cache when it has it. Null when it cannot be had. */
private suspend fun coverAt(context: Context, url: String): ImageBitmap? {
    val request = ImageRequest.Builder(context)
        .data(url)
        .addHeader("Authorization", "Bearer ${ApiClient.getToken() ?: ""}")
        .size(WALL_COVER_PX)
        .allowHardware(false)
        .build()
    return (context.imageLoader.execute(request) as? SuccessResult)?.drawable?.toBitmap()?.asImageBitmap()
}

private const val WALL_COVERS = 40
private const val WALL_COVER_PX = 240
