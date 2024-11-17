package com.paulohenriquesg.fahrenheit

import android.media.MediaPlayer

object GlobalMediaPlayer {
    private var mediaPlayer: MediaPlayer? = null

    fun getInstance(): MediaPlayer {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer()
        }
        return mediaPlayer!!
    }

    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}