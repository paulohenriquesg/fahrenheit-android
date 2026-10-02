package com.paulohenriquesg.fahrenheit.player

import android.content.Context
import android.os.Build
import com.paulohenriquesg.fahrenheit.BuildConfig
import com.paulohenriquesg.fahrenheit.R
import com.paulohenriquesg.fahrenheit.api.PlayLibraryItemDeviceInfo
import java.util.UUID

/**
 * How this device introduces itself when it opens a listening session.
 *
 * The id is this install's own, made once and kept. The server closes a
 * user's open sessions on the same device when another starts, so a shared
 * id made two TVs on one account close each other's sessions.
 */
object PlaybackDevice {
    private const val PREFS = "playback_device"
    private const val KEY_ID = "device_id"

    fun info(context: Context) = PlayLibraryItemDeviceInfo(
        deviceId = id(context),
        clientName = context.getString(R.string.app_name),
        clientVersion = BuildConfig.VERSION_NAME,
        manufacturer = Build.MANUFACTURER,
        model = Build.MODEL,
        sdkVersion = Build.VERSION.SDK_INT
    )

    private fun id(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.getString(KEY_ID, null)?.let { return it }
        return UUID.randomUUID().toString().also { prefs.edit().putString(KEY_ID, it).apply() }
    }

    /** As if this were another install. */
    internal fun forgetForTest(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().commit()
    }
}
