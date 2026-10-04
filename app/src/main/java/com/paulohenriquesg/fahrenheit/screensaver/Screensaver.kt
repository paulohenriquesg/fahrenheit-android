package com.paulohenriquesg.fahrenheit.screensaver

import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.paulohenriquesg.fahrenheit.ui.elements.CoverImage
import com.paulohenriquesg.fahrenheit.ui.elements.CoverWall
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * What the now-playing line says: the title, then the chapter and time left
 * in it (a book) or the time left (an episode), as [nowPlayingDetail] writes it.
 */
data class NowPlayingLine(val itemId: String, val title: String, val detail: String?)

/** What the playback service has queued, and whether it plays. */
data class Queued(val itemId: String, val playing: Boolean)

/**
 * The screensaver's art.
 *
 * @property covers the wall's: the series being played first, then the library's covers this device holds.
 * @property wash the playing cover's colour, glowing behind the bouncing cover; null for none.
 */
data class WallArt(val covers: List<ImageBitmap>, val wash: Color?)

/** Where the screensaver learns what plays. */
interface ListeningSource {
    /** What is queued and whether it plays; null for nothing. */
    @Composable
    fun queued(): Queued?

    /** The now-playing line. */
    @Composable
    fun line(): NowPlayingLine?

    /** The wall's covers and the playing cover's colour. */
    suspend fun art(itemId: String): WallArt
}

object ScreensaverTags {
    const val SCREEN = "screensaver"
    const val OVERLAY = "screensaver-overlay"
    const val WALL = "screensaver-wall"
    const val BOUNCING_COVER = "screensaver-bouncing-cover"
    const val DIM = "screensaver-dim"
    const val GLOW = "screensaver-glow"
}

/**
 * The listening screensaver on a screen (#156; docs/mocks/screensaver.html).
 *
 * An overlay over the screen's own content keeps the screen on and draws the
 * screensaver when [ScreensaverPolicy] says, and a wrapper round the window's
 * key handling feeds [KeyGate], so the first key only wakes the screen.
 *
 * Nothing in it runs on a timer: it waits once for the next change
 * ([ScreensaverPolicy.nextChangeMs]), again after any key or change of play,
 * and moves only while the screensaver is showing.
 */
object Screensaver {

    fun install(
        activity: ComponentActivity,
        source: ListeningSource,
        settings: () -> ScreensaverSettings,
        lastKey: LastKey
    ) {
        val window = activity.window
        // Once per screen: a second wrapper would count every key twice.
        if (window.decorView.findViewWithTag<android.view.View>(ScreensaverTags.OVERLAY) != null) return
        val clock = lastKey.clock
        val gate = KeyGate(lastKey)
        val keys = KeyCount()
        window.callback = GatedKeys(window.callback, gate, keys)
        val overlay = ComposeView(activity).apply {
            tag = ScreensaverTags.OVERLAY
            // It draws; it never takes focus or the remote's keys. Compose's
            // own view inside is focusable by default, and taking focus when
            // the screensaver appears would leave the screen under it deaf
            // to the remote: blocked here for everything inside.
            isFocusable = false
            isFocusableInTouchMode = false
            descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
            // keepScreenOn on this view, which stays attached: Compose's own
            // inner view is not the one other code (and tests) look at.
            setContent { FahrenheitTheme { Overlay(gate, keys, source, settings, clock) { keepScreenOn = it } } }
        }
        activity.addContentView(
            overlay,
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
    }

    @Composable
    private fun Overlay(
        gate: KeyGate,
        keys: KeyCount,
        source: ListeningSource,
        settings: () -> ScreensaverSettings,
        clock: () -> Long,
        keepOn: (Boolean) -> Unit
    ) {
        // The setting, read again at every key. Off: the player is not even
        // asked what plays, and nothing here runs.
        val chosen = remember(keys.count) { settings() }
        val now = if (chosen.delayMinutes != null) source.queued() else null
        val playing = now?.playing == true
        val queued = now != null
        var wasPlaying by remember { mutableStateOf<Boolean?>(null) }
        var notPlayingSince by remember { mutableStateOf<Long?>(null) }
        var decision by remember { mutableStateOf(ScreensaverPolicy.Decision(keepScreenOn = false, show = false)) }

        LaunchedEffect(playing, queued) {
            // Counted only from a pause seen here: a screen opened onto
            // something paused an hour ago has no reason to hold the screen.
            notPlayingSince = when {
                playing || !queued -> null
                wasPlaying == true -> clock()
                else -> notPlayingSince
            }
            wasPlaying = if (queued) playing else null
        }

        // Settled again after every key, change of play, or change of setting
        // seen on the way: decide now, wait once for the next change, decide
        // again. At most two waits, then it rests until something happens.
        LaunchedEffect(keys.count, playing, queued, chosen, notPlayingSince) {
            while (true) {
                val idle = gate.idleMs()
                val stopped = notPlayingSince?.let { clock() - it }
                decision = ScreensaverPolicy.decide(chosen.delayMinutes, playing, idle, stopped)
                    .let { if (queued) it else ScreensaverPolicy.Decision(false, false) }
                gate.showing = decision.show
                keepOn(decision.keepScreenOn)
                val wait = ScreensaverPolicy.nextChangeMs(chosen.delayMinutes, playing, idle, stopped) ?: break
                delay(wait)
            }
        }

        if (decision.show && now != null) {
            // Only now: the line polls the position, and the art is fetches
            // and a decode. Gone again when it hides.
            val art by produceState<WallArt?>(null, now.itemId) { value = source.art(now.itemId) }
            val line = source.line()
            if (line != null) ScreensaverScreen(chosen.style, line, art ?: WallArt(emptyList(), null))
        }
    }

    /** Every key, counted, so the overlay settles again at once. */
    private class KeyCount {
        var count by mutableLongStateOf(0L)
    }

    /** The window's own key handling, with the screensaver's say first. */
    private class GatedKeys(
        private val wrapped: Window.Callback,
        private val gate: KeyGate,
        private val keys: KeyCount
    ) : Window.Callback by wrapped {
        override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
            gate.activity()
            keys.count++
            Snapshot.sendApplyNotifications()
            return wrapped.dispatchGenericMotionEvent(event)
        }

        override fun dispatchKeyEvent(event: KeyEvent): Boolean {
            val eaten = gate.key(down = event.action == KeyEvent.ACTION_DOWN, keyCode = event.keyCode)
            keys.count++
            // Written outside Compose: tell it now, so the overlay settles on
            // its next frame rather than whenever Compose next looks.
            Snapshot.sendApplyNotifications()
            return eaten || wrapped.dispatchKeyEvent(event)
        }
    }
}

/**
 * The screensaver itself, in either style. Everything is dimmed and moves:
 * the wall drifts, the cover bounces, and on the wall the now-playing line
 * moves to another corner every minute.
 *
 * @param elapsedMs how long it has shown; null to count it from the frames,
 *   which only happens while it is on screen. Tests pass a time.
 */
@Composable
fun ScreensaverScreen(style: ScreensaverStyle, line: NowPlayingLine, art: WallArt, elapsedMs: Long? = null) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clipToBounds()
            .testTag(ScreensaverTags.SCREEN)
    ) {
        when (style) {
            ScreensaverStyle.Wall -> {
                // #168's wall: drawn once, drifting by moving its layer.
                CoverWall(art.covers, Modifier.fillMaxSize().testTag(ScreensaverTags.WALL), alpha = 0.38f)
                // An even dim, and no colour: the playing cover's wash over
                // many covers reads as a stain (#172). It stays on the bouncing
                // cover, behind one cover on a dark screen.
                Box(Modifier.fillMaxSize().testTag(ScreensaverTags.DIM).background(Color.Black.copy(alpha = 0.45f)))
                // The corner changes once a minute: so does this, not every frame.
                val minutes = elapsedMs ?: produceState(0L) {
                    while (true) {
                        delay(CORNER_MS)
                        value += CORNER_MS
                    }
                }.value
                CornerLine(line, NowPlayingCorner.at(minutes))
            }
            ScreensaverStyle.Bouncing -> BouncingCover(line, art.wash, elapsedMs)
        }
    }
}

private const val CORNER_MS = 60_000L

/** The thumb, title and chapter line, small and dim, in one corner. */
@Composable
private fun CornerLine(line: NowPlayingLine, corner: NowPlayingCorner) {
    val alignment = when (corner) {
        NowPlayingCorner.TopStart -> Alignment.TopStart
        NowPlayingCorner.TopEnd -> Alignment.TopEnd
        NowPlayingCorner.BottomEnd -> Alignment.BottomEnd
        NowPlayingCorner.BottomStart -> Alignment.BottomStart
    }
    Box(Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 40.dp), contentAlignment = alignment) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.alpha(0.55f).widthIn(max = 420.dp)
        ) {
            CoverImage(itemId = line.itemId, contentDescription = null, size = 42.dp, title = line.title)
            Spacer(Modifier.width(11.dp))
            Column {
                Text(line.title, style = MaterialTheme.typography.titleMedium, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                line.detail?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = Color(0xFFCAC4D0), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

private val BOUNCING_COVER = 160.dp
private val BOUNCING_GLOW = 360.dp

/**
 * The playing cover bouncing off the edges, its chapter and time left under
 * it, and the cover's [wash] glowing behind it. The time is read only where
 * the block is placed, so a frame moves it without composing anything again.
 */
@Composable
private fun BouncingCover(line: NowPlayingLine, wash: Color?, elapsedMs: Long?) {
    val clock = remember { mutableLongStateOf(elapsedMs ?: 0L) }
    if (elapsedMs == null) {
        LaunchedEffect(Unit) {
            val start = withFrameMillis { it }
            while (true) withFrameMillis { clock.longValue = it - start }
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        // The cover and its caption, as one block that bounces.
        val blockPx = with(LocalDensity.current) { (BOUNCING_COVER + 40.dp).toPx() }
        if (wash != null) {
            // Its own box round the cover, not drawn past the cover's bounds.
            val inset = with(LocalDensity.current) { ((BOUNCING_GLOW - BOUNCING_COVER) / 2).toPx() }
            Box(
                Modifier
                    .offset {
                        val (x, y) = Bounce.at(clock.longValue, width, height, blockPx)
                        IntOffset((x - inset).roundToInt(), (y - inset).roundToInt())
                    }
                    .requiredSize(BOUNCING_GLOW)
                    .testTag(ScreensaverTags.GLOW)
                    .background(Brush.radialGradient(listOf(wash.copy(alpha = 0.45f), Color.Transparent)))
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .offset {
                    val (x, y) = Bounce.at(clock.longValue, width, height, blockPx)
                    IntOffset(x.roundToInt(), y.roundToInt())
                }
                .width(BOUNCING_COVER)
                .alpha(0.8f)
        ) {
            Box(Modifier.testTag(ScreensaverTags.BOUNCING_COVER)) {
                CoverImage(itemId = line.itemId, contentDescription = null, size = BOUNCING_COVER, title = line.title)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = line.detail ?: line.title,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFCAC4D0),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
