package com.paulohenriquesg.fahrenheit

import android.net.Uri
import com.paulohenriquesg.fahrenheit.R
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import com.paulohenriquesg.fahrenheit.api.Chapter
import com.paulohenriquesg.fahrenheit.player.PlaybackPosition
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MediaPlayerController(
    url: String,
    mediaSession: MediaSessionCompat,
    isPlaying: Boolean,
    onPlayPause: (Boolean) -> Unit,
    duration: Double = 0.0,
    currentTime: Double = 0.0,
    chapters: List<Chapter>? = null,
    authToken: String? = null,
    shouldAutoPlay: Boolean = false,
    onCurrentTimeUpdate: (Double) -> Unit = {}
) {
    val context = LocalContext.current
    val mediaPlayer = remember { GlobalMediaPlayer.getInstance() }
    var progress by remember { mutableStateOf(PlaybackPosition.fraction(currentTime, duration)) }
    var currentTimeState by remember { mutableStateOf(currentTime) }
    var totalTime by remember { mutableStateOf(duration) }
    var sliderSize by remember { mutableStateOf(IntSize.Zero) }
    var isPrepared by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(url) {
        android.util.Log.d("MediaPlayerController", "LaunchedEffect url: $url")
        android.util.Log.d("MediaPlayerController", "duration: $duration, currentTime: $currentTime, authToken: ${authToken?.take(20)}...")
        isPrepared = false
        mediaPlayer.apply {
            reset() // Ensure the media player is reset before setting a new data source
            android.util.Log.d("MediaPlayerController", "MediaPlayer reset")

            setOnErrorListener { mp, what, extra ->
                android.util.Log.e("MediaPlayerController", "MediaPlayer error - what: $what, extra: $extra")
                false
            }

            if (authToken != null) {
                val headers = mapOf("Authorization" to "Bearer $authToken")
                android.util.Log.d("MediaPlayerController", "Setting data source with auth headers")
                setDataSource(context, url.toUri(), headers)
            } else {
                android.util.Log.d("MediaPlayerController", "Setting data source without auth")
                setDataSource(url)
            }
            android.util.Log.d("MediaPlayerController", "Data source set, calling prepareAsync")

            setOnPreparedListener {
                android.util.Log.d("MediaPlayerController", "OnPreparedListener called, shouldAutoPlay: $shouldAutoPlay")
                // Use the parameter duration (from API) if available, otherwise use MediaPlayer duration
                if (duration > 0) {
                    totalTime = duration
                } else {
                    totalTime = mediaPlayer.duration / 1000.0
                }
                currentTimeState = currentTime
                progress = PlaybackPosition.fraction(currentTime, totalTime)
                android.util.Log.d("MediaPlayerController", "Seeking to position: ${(currentTime * 1000).toInt()}ms")
                seekTo((currentTime * 1000).toInt()) // Seek to the currentTime position
                onCurrentTimeUpdate(currentTime) // Notify parent of initial position
                isPrepared = true
                android.util.Log.d("MediaPlayerController", "MediaPlayer prepared, totalTime: $totalTime, currentTimeState: $currentTimeState")

                // Auto-play if requested
                if (shouldAutoPlay) {
                    android.util.Log.d("MediaPlayerController", "Auto-playing after preparation")
                    onPlayPause(true)
                }
            }
            prepareAsync()
        }
    }

    LaunchedEffect(isPlaying) {
        android.util.Log.d("MediaPlayerController", "isPlaying changed to: $isPlaying, isPrepared: $isPrepared, mediaPlayer.isPlaying: ${mediaPlayer.isPlaying}")
        if (isPlaying) {
            if (isPrepared && !mediaPlayer.isPlaying) {
                android.util.Log.d("MediaPlayerController", "Starting media player")
                mediaPlayer.start()
                android.util.Log.d("MediaPlayerController", "Media player started, isPlaying: ${mediaPlayer.isPlaying}")
            } else {
                android.util.Log.d("MediaPlayerController", "Cannot start - isPrepared: $isPrepared, mediaPlayer.isPlaying: ${mediaPlayer.isPlaying}")
            }
            coroutineScope.launch {
                while (isPlaying && isPrepared) {
                    currentTimeState = mediaPlayer.currentPosition / 1000.0
                    progress = PlaybackPosition.fraction(currentTimeState, totalTime)
                    onCurrentTimeUpdate(currentTimeState)
                    delay(1000L)
                }
            }
        } else {
            if (mediaPlayer.isPlaying) {
                android.util.Log.d("MediaPlayerController", "Pausing media player")
                mediaPlayer.pause()
            }
        }
    }

    Column(modifier = Modifier.padding(8.dp)) {
        Row(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Skip Back 30s
            FilledTonalIconButton(
                onClick = {
                    if (isPrepared) {
                        val newPosition = PlaybackPosition.skip(currentTimeState, -SKIP_SECONDS, totalTime)
                        mediaPlayer.seekTo((newPosition * 1000).toInt())
                        currentTimeState = newPosition
                        progress = PlaybackPosition.fraction(newPosition, totalTime)
                        onCurrentTimeUpdate(newPosition)
                    }
                },
                enabled = isPrepared,
                modifier = Modifier.size(48.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(Icons.Filled.FastRewind, contentDescription = stringResource(R.string.skip_back_30_seconds))
            }

            // Play/Pause (larger)
            FilledIconButton(
                onClick = {
                    if (isPrepared) {
                        onPlayPause(!isPlaying)
                    }
                },
                enabled = isPrepared,
                modifier = Modifier.size(56.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(
                    if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play"
                )
            }

            // Stop
            IconButton(
                onClick = {
                    mediaPlayer.stop()
                    mediaPlayer.reset()
                    isPrepared = false
                    if (authToken != null) {
                        val headers = mapOf("Authorization" to "Bearer $authToken")
                        mediaPlayer.setDataSource(context, url.toUri(), headers)
                    } else {
                        mediaPlayer.setDataSource(url)
                    }
                    mediaPlayer.prepareAsync()
                    onPlayPause(false)
                },
                enabled = isPrepared,
                modifier = Modifier.size(48.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(Icons.Filled.Stop, contentDescription = stringResource(R.string.stop))
            }

            // Skip Forward 30s
            FilledTonalIconButton(
                onClick = {
                    if (isPrepared) {
                        val newPosition = PlaybackPosition.skip(currentTimeState, SKIP_SECONDS, totalTime)
                        mediaPlayer.seekTo((newPosition * 1000).toInt())
                        currentTimeState = newPosition
                        progress = PlaybackPosition.fraction(newPosition, totalTime)
                        onCurrentTimeUpdate(newPosition)
                    }
                },
                enabled = isPrepared,
                modifier = Modifier.size(48.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(Icons.Filled.FastForward, contentDescription = stringResource(R.string.skip_forward_30_seconds))
            }
        }

        Box(modifier = Modifier.padding(top = 16.dp)) {
            Slider(
                value = progress,
                onValueChange = { newValue ->
                    progress = newValue
                    mediaPlayer.seekTo((newValue * totalTime * 1000).toInt())
                    currentTimeState = mediaPlayer.currentPosition / 1000.0
                    onCurrentTimeUpdate(currentTimeState)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates ->
                        sliderSize = coordinates.size
                    }
            )

            val chapterColor = MaterialTheme.colorScheme.onSurfaceVariant
            Canvas(modifier = Modifier.matchParentSize()) {
                PlaybackPosition.chapterMarks(chapters, totalTime).forEach { percentage ->
                    drawLineAtPercentage(percentage, sliderSize.width, 4.dp.toPx(), chapterColor)
                }
            }
        }

        Row(
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text(
                text = "Current Time: ${PlaybackPosition.clock(currentTimeState)}",
                modifier = Modifier.weight(1f)
                , color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Total Time: ${PlaybackPosition.clock(totalTime)}",
                modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

fun DrawScope.drawLineAtPercentage(percentage: Float, sliderWidth: Int, trackHeight: Float, color: androidx.compose.ui.graphics.Color) {
    val position = (percentage / 100) * sliderWidth
    drawLine(
        color = color,
        start = Offset(x = position, y = (size.height - trackHeight) / 2),
        end = Offset(x = position, y = (size.height + trackHeight) / 2),
        strokeWidth = 2f
    )
}

/** How far the skip buttons jump. */
private const val SKIP_SECONDS = 30.0
