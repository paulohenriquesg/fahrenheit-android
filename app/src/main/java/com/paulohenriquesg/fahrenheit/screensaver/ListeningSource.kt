package com.paulohenriquesg.fahrenheit.screensaver

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.paulohenriquesg.fahrenheit.R
import com.paulohenriquesg.fahrenheit.api.ApiClient
import com.paulohenriquesg.fahrenheit.api.LibraryRepository
import com.paulohenriquesg.fahrenheit.login.LoginActivity
import com.paulohenriquesg.fahrenheit.player.Playback
import com.paulohenriquesg.fahrenheit.player.PlayerSettings
import com.paulohenriquesg.fahrenheit.player.QueuedFile
import com.paulohenriquesg.fahrenheit.player.RailEntry
import com.paulohenriquesg.fahrenheit.player.coverBitmap
import com.paulohenriquesg.fahrenheit.player.coverWashOf
import com.paulohenriquesg.fahrenheit.player.rememberRailEntry
import com.paulohenriquesg.fahrenheit.ui.elements.RecentCovers
import com.paulohenriquesg.fahrenheit.utils.listeningLength
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.util.Locale
import kotlin.math.ceil

/**
 * Puts the listening screensaver (#156) on a screen of the app: called as
 * each screen starts, after its own content is set, so the overlay sits on
 * top. The sign-in screen has nothing playing and is left alone.
 */
fun installListeningScreensaver(activity: Activity) {
    if (activity !is ComponentActivity || activity is LoginActivity) return
    val settings = PlayerSettings(activity)
    Screensaver.install(
        activity,
        PlaybackListening(activity),
        settings = { ScreensaverSettings(settings.screensaverMinutes, settings.screensaverStyle) },
        lastKey = LastKey.app
    )
}

/**
 * What the playback service has queued, through a controller of this
 * screen's own: connected while the screen is started and let go when it
 * stops, as ControllerSlot does, so a screen in the back stack does not keep
 * the service bound.
 */
private class PlaybackListening(private val context: Context) : ListeningSource {
    private var controller by mutableStateOf<MediaController?>(null)

    /** From the player's events only: nothing polls while the screensaver waits. */
    @Composable
    override fun queued(): Queued? {
        val lifecycle = LocalLifecycleOwner.current.lifecycle
        DisposableEffect(lifecycle) {
            var future: com.google.common.util.concurrent.ListenableFuture<MediaController>? = null
            fun connect() {
                if (future != null) return
                future = Playback.connect(context).also { f ->
                    f.addListener({ controller = runCatching { f.get() }.getOrNull() }, ContextCompat.getMainExecutor(context))
                }
            }
            fun release() {
                controller = null
                future?.let(MediaController::releaseFuture)
                future = null
            }
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> connect()
                    Lifecycle.Event.ON_STOP -> release()
                    else -> Unit
                }
            }
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) connect()
            lifecycle.addObserver(observer)
            onDispose {
                lifecycle.removeObserver(observer)
                release()
            }
        }
        val player = controller ?: return null
        var queued by remember(player) { mutableStateOf(queuedOf(player)) }
        DisposableEffect(player) {
            val listener = object : Player.Listener {
                override fun onEvents(player: Player, events: Player.Events) {
                    queued = queuedOf(player)
                }
            }
            player.addListener(listener)
            onDispose { player.removeListener(listener) }
        }
        return queued
    }

    /** As the rail's entry reads it; composed only while the screensaver shows. */
    @Composable
    override fun line(): NowPlayingLine? {
        val entry = rememberRailEntry(controller, ::chaptersOf) ?: return null
        val detail = nowPlayingDetail(
            context.resources, entry.chapter, entry.chapterNumber, entry.episodeId != null, entry.leftSeconds
        )
        return NowPlayingLine(entry.itemId, entry.title, detail)
    }

    override suspend fun art(itemId: String): WallArt =
        WallArt(wallCovers(context, itemId), coverWashOf(coverBitmap(context, itemId)))
}

/** As the rail reads it: the queued file, and "playing" as asked for, so buffering counts. */
private fun queuedOf(player: Player): Queued? =
    QueuedFile.of(player.currentMediaItem)?.let { Queued(it.itemId, player.playWhenReady) }

private suspend fun chaptersOf(itemId: String) =
    ApiClient.getLibraryApi()?.let { LibraryRepository(it).item(itemId).getOrNull() }?.media?.chapters

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

/**
 * "Chapter 12 · 18 min left in chapter" for a book, "25 min left" for an
 * episode or a book without chapters (#172). Whole minutes, rounded up and
 * never under one, so it does not say "0 min" while something plays.
 */
internal fun nowPlayingDetail(
    resources: Resources,
    chapter: String?,
    chapterNumber: Int?,
    episode: Boolean,
    leftSeconds: Double
): String {
    val left = resources.getString(R.string.time_left, wholeMinutes(leftSeconds))
    if (episode) return left
    val name = chapter ?: chapterNumber?.let { resources.getString(R.string.chapter_number, it) } ?: return left
    return resources.getString(R.string.screensaver_chapter_left, name, wholeMinutes(leftSeconds))
}

/** "18 min", "1 h 5 min", "2 h": as listeningLength writes it, in whole minutes. */
private fun wholeMinutes(seconds: Double): String {
    val minutes = ceil(seconds / 60).toLong().coerceAtLeast(1)
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0L -> String.format(Locale.ROOT, "%d min", m)
        m == 0L -> String.format(Locale.ROOT, "%d h", h)
        else -> String.format(Locale.ROOT, "%d h %d min", h, m)
    }
}
