package com.paulohenriquesg.fahrenheit

import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale


@Composable
fun MediaPlayerController(
    url: String,
    mediaSession: MediaSessionCompat,
    isPlaying: Boolean,
    onPlayPause: (Boolean) -> Unit,
    duration: Float = 0f,
    currentTime: Float = 0f,
) {
    Log.d("MediaPlayerController", "URL: $url")

    val mediaPlayer = remember { GlobalMediaPlayer.getInstance() }
    var progress by remember { mutableStateOf(if (duration > 0) currentTime / duration else 0f) }
    var currentTimeState by remember { mutableStateOf(currentTime) }
    var totalTime by remember { mutableStateOf(duration) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(url) {
        mediaPlayer.apply {
            setDataSource(url)
            setOnPreparedListener {
                totalTime = duration
                currentTimeState = currentTime
                progress = if (duration > 0) currentTime / duration else 0f
                seekTo((currentTime * 1000).toInt()) // Seek to the currentTime position
            }
            prepareAsync()
        }
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            mediaPlayer.start()
            coroutineScope.launch {
                while (isPlaying) {
                    currentTimeState = mediaPlayer.currentPosition / 1000f
                    progress = if (totalTime > 0) currentTimeState / totalTime else 0f
                    delay(1000L)
                }
            }
        } else {
            mediaPlayer.pause()
        }
    }

    Column(modifier = Modifier.padding(8.dp)) {
        Row(
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Button(
                onClick = {
                    if (isPlaying) {
                        mediaPlayer.pause()
                    } else {
                        mediaPlayer.start()
                        mediaPlayer.seekTo((currentTimeState * 1000).toInt()) // Seek to the currentTime position
                    }
                    onPlayPause(!isPlaying)
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(if (isPlaying) "Pause" else "Play")
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    mediaPlayer.stop()
                    mediaPlayer.reset()
                    mediaPlayer.setDataSource(url)
                    mediaPlayer.prepareAsync()
                    onPlayPause(false)
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Stop")
            }
        }

        Slider(
            value = progress,
            onValueChange = { newValue ->
                progress = newValue
                mediaPlayer.seekTo((newValue * totalTime * 1000).toInt())
                currentTimeState = mediaPlayer.currentPosition / 1000f
            },
            modifier = Modifier.padding(top = 16.dp)
        )

        Row(
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text(
                text = "Current Time: ${formatTime(currentTimeState.toInt()*1000)}",
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "Total Time: ${formatTime(totalTime.toInt()*1000)}",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

fun formatTime(milliseconds: Int): String {
    val hours = (milliseconds / 1000) / 3600
    val minutes = ((milliseconds / 1000) % 3600) / 60
    val seconds = (milliseconds / 1000) % 60
    return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
}