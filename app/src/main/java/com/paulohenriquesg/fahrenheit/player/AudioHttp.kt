package com.paulohenriquesg.fahrenheit.player

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import okhttp3.OkHttpClient

/**
 * Where ExoPlayer gets audio: the app's own authenticated OkHttp client, which
 * adds the current access token to every request and refreshes it on a 401.
 *
 * The client is looked up each time a file is opened, not once: a book can
 * outlive a sign-out and sign-in, and the next file must go out as whoever is
 * signed in now.
 */
@OptIn(UnstableApi::class) // OkHttpDataSource
object AudioHttp {
    private val signedOut by lazy { OkHttpClient() }

    fun dataSourceFactory(client: () -> OkHttpClient?): DataSource.Factory = DataSource.Factory {
        OkHttpDataSource.Factory(client() ?: signedOut).createDataSource()
    }
}
