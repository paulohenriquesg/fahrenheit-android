package com.paulohenriquesg.fahrenheit.player

import android.content.Context
import android.os.Build
import com.paulohenriquesg.fahrenheit.BuildConfig
import com.paulohenriquesg.fahrenheit.R
import com.paulohenriquesg.fahrenheit.api.PlayLibraryItemDeviceInfo

/**
 * How this device introduces itself when it opens a listening session. The
 * id is unchanged from before, so the server's device records stay as they
 * were; the rest used to be hard-coded.
 */
object PlaybackDevice {
    fun info(context: Context) = PlayLibraryItemDeviceInfo(
        deviceId = "Fire Stick",
        clientName = context.getString(R.string.app_name),
        clientVersion = BuildConfig.VERSION_NAME,
        manufacturer = Build.MANUFACTURER,
        model = Build.MODEL,
        sdkVersion = Build.VERSION.SDK_INT
    )
}
