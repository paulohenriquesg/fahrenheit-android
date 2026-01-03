package com.paulohenriquesg.fahrenheit

import android.net.Uri
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun MediaPlayerController(
    url: String,
    mediaSession: MediaSessionCompat,
    isPlaying: Boolean,
    onPlayPause: (Boolean) -> Unit,
    duration: Double = 0.0,
    currentTime: Double = 0.0,
    chapters: List<Chapter>? = null,
    authToken: String? = null
) {
    val context = LocalContext.current
    val mediaPlayer = remember { GlobalMediaPlayer.getInstance() }
    var progress by remember { mutableStateOf(if (duration > 0) (currentTime / duration).toFloat() else 0f) }
    var currentTimeState by remember { mutableStateOf(currentTime) }
    var totalTime by remember { mutableStateOf(duration) }
    var sliderSize by remember { mutableStateOf(IntSize.Zero) }
    var isPrepared by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(url) {
        isPrepared = false
        mediaPlayer.apply {
            reset() // Ensure the media player is reset before setting a new data source
            if (authToken != null) {
                val headers = mapOf("Authorization" to "Bearer $authToken")
                setDataSource(context, Uri.parse(url), headers)
            } else {
                setDataSource(url)
            }
            setOnPreparedListener {
                // Use the parameter duration (from API) if available, otherwise use MediaPlayer duration
                if (duration > 0) {
                    totalTime = duration
                } else {
                    totalTime = mediaPlayer.duration / 1000.0
                }
                currentTimeState = currentTime
                progress = if (totalTime > 0) (currentTime / totalTime).toFloat() else 0f
                seekTo((currentTime * 1000).toInt()) // Seek to the currentTime position
                isPrepared = true
            }
            prepareAsync()
        }
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            if (isPrepared && !mediaPlayer.isPlaying) {
                mediaPlayer.start()
            }
            coroutineScope.launch {
                while (isPlaying && isPrepared) {
                    currentTimeState = mediaPlayer.currentPosition / 1000.0
                    progress = if (totalTime > 0) (currentTimeState / totalTime).toFloat() else 0f
                    delay(1000L)
                }
            }
        } else {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.pause()
            }
        }
    }

    Column(modifier = Modifier.padding(8.dp)) {
        Row(
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Button(
                onClick = {
                    if (isPrepared) {
                        onPlayPause(!isPlaying)
                    }
                },
                enabled = isPrepared,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text(if (isPlaying) "Pause" else "Play", color = MaterialTheme.colorScheme.onPrimary)
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    mediaPlayer.stop()
                    mediaPlayer.reset()
                    isPrepared = false
                    if (authToken != null) {
                        val headers = mapOf("Authorization" to "Bearer $authToken")
                        mediaPlayer.setDataSource(context, Uri.parse(url), headers)
                    } else {
                        mediaPlayer.setDataSource(url)
                    }
                    mediaPlayer.prepareAsync()
                    onPlayPause(false)
                },
                enabled = isPrepared,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text("Stop", color = MaterialTheme.colorScheme.onSecondary)
            }
        }

        Box(modifier = Modifier.padding(top = 16.dp)) {
            Slider(
                value = progress,
                onValueChange = { newValue ->
                    progress = newValue
                    mediaPlayer.seekTo((newValue * totalTime * 1000).toInt())
                    currentTimeState = mediaPlayer.currentPosition / 1000.0
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates ->
                        sliderSize = coordinates.size
                    }
            )

            val chapterColor = MaterialTheme.colorScheme.onSurfaceVariant
            Canvas(modifier = Modifier.matchParentSize()) {
                chapters?.dropLast(1)?.forEach { chapter ->
                    chapter.end?.let { end ->
                        val percentage = (end / totalTime) * 100
                        drawLineAtPercentage(percentage.toFloat(), sliderSize.width, 4.dp.toPx(), chapterColor)
                    }
                }
            }
        }

        Row(
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text(
                text = "Current Time: ${formatTime(currentTimeState.toInt() * 1000)}",
                modifier = Modifier.weight(1f)
                , color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Total Time: ${formatTime(totalTime.toInt() * 1000)}",
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

fun formatTime(milliseconds: Int): String {
    val hours = (milliseconds / 1000) / 3600
    val minutes = ((milliseconds / 1000) % 3600) / 60
    val seconds = (milliseconds / 1000) % 60
    return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
}