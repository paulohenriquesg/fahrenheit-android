package com.paulohenriquesg.fahrenheit.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.media3.common.Player
import com.paulohenriquesg.fahrenheit.api.Chapter
import kotlinx.coroutines.delay

/**
 * What the rail's Now playing entry says about what is queued (#107; the
 * rail frames of docs/mocks/player.html).
 *
 * @property progress how far through the whole book or episode, 0..1: the ring.
 * @property chapter the title of the chapter playing; null for none, or an untitled one.
 * @property chapterNumber the chapter's number, for a book or episode with chapters.
 * @property leftSeconds what is left of the chapter, or of the whole when
 *   there is no chapter, at the speed - as the player counts it.
 */
data class RailEntry(
    val itemId: String,
    val episodeId: String?,
    val title: String,
    val playing: Boolean,
    val progress: Float,
    val chapter: String?,
    val chapterNumber: Int? = null,
    val leftSeconds: Double
) {
    companion object {
        /**
         * Whether the entry shows it playing (#179): play is wanted, and the
         * player can play - or is buffering, so a seek does not flash the
         * pause badge. An ended player still wants to play; it is not playing.
         */
        fun showsPlaying(playWhenReady: Boolean, state: Int): Boolean =
            playWhenReady && (state == Player.STATE_READY || state == Player.STATE_BUFFERING)

        /** Null when nothing of ours is queued. */
        fun of(
            file: QueuedFile?,
            title: String?,
            positionInFile: Double,
            playing: Boolean,
            speed: Float,
            spans: List<ChapterSpan>
        ): RailEntry? {
            file ?: return null
            val at = file.bookTime(positionInFile)
            val span = ChapterClock.at(spans, at)
            val chapter = span?.title?.takeIf { it.isNotBlank() }
            val number = span?.let { spans.indexOf(it) + 1 }
            val left = span?.left(at) ?: PlaybackPosition.left(at, file.bookTotal)
            return RailEntry(
                itemId = file.itemId,
                episodeId = file.episodeId,
                title = title.orEmpty(),
                playing = playing,
                progress = PlaybackPosition.fraction(at, file.bookTotal),
                chapter = chapter,
                chapterNumber = number,
                leftSeconds = left / speed
            )
        }
    }
}

/**
 * The rail's entry for what [player] - the main screen's controller - has
 * queued; null when nothing is, or there is no player. It follows the
 * player's events, and its position only while playing (a loop that never
 * ends would keep Compose from idling). A book's or episode's chapters are
 * asked for once.
 */
@Composable
fun rememberRailEntry(player: Player?, chaptersOf: suspend (itemId: String, episodeId: String?) -> List<Chapter>?): RailEntry? {
    if (player == null) return null
    var changes by remember(player) { mutableIntStateOf(0) }
    var playing by remember(player) { mutableStateOf(player.isPlaying) }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                changes++
                playing = player.isPlaying
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }
    LaunchedEffect(player, playing) {
        while (playing) {
            delay(RAIL_POLL_MS)
            changes++
        }
    }
    // Read on every change.
    changes.let { }
    val file = QueuedFile.of(player.currentMediaItem)
    val known = file?.let { RailChapters.key(it.itemId, it.episodeId) }
    var spans by remember(known) { mutableStateOf(known?.let { RailChapters.known[it] }.orEmpty()) }
    LaunchedEffect(known) {
        if (file == null || known == null || known in RailChapters.known) return@LaunchedEffect
        spans = ChapterClock.spans(chaptersOf(file.itemId, file.episodeId), file.bookTotal)
            .also { RailChapters.known[known] = it }
    }
    return RailEntry.of(
        file = file,
        title = player.currentMediaItem?.mediaMetadata?.title?.toString(),
        positionInFile = player.currentPosition / 1000.0,
        playing = RailEntry.showsPlaying(player.playWhenReady, player.playbackState),
        speed = player.playbackParameters.speed,
        spans = spans
    )
}

/** How often a playing entry is read again; the equaliser runs this long on each one. */
internal const val RAIL_POLL_MS = 5_000L

/**
 * Books' and episodes' chapters, once asked for: the main screen gets a new
 * controller on every return to it, and should not fetch the item each time.
 * Main thread only.
 */
internal object RailChapters {
    val known = mutableMapOf<String, List<ChapterSpan>>()

    fun key(itemId: String, episodeId: String?) = if (episodeId == null) itemId else "$itemId/$episodeId"
}
