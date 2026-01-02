package com.paulohenriquesg.fahrenheit

import android.media.MediaPlayer

object GlobalMediaPlayer {
    private var mediaPlayer: MediaPlayer? = null

    fun getInstance(): MediaPlayer {
        return mediaPlayer ?: MediaPlayer().also { mediaPlayer = it }
    }

    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}