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
    private const val KEY_NAME = "device_name"
    const val MAX_NAME_LENGTH = 40

    /** What the listener called this TV, or its model until they do. */
    fun name(context: Context): String = prefs(context).getString(KEY_NAME, null) ?: Build.MODEL

    /** A blank name goes back to the model. The id never changes with it. */
    fun setName(context: Context, name: String) {
        val trimmed = name.trim().take(MAX_NAME_LENGTH)
        prefs(context).edit().apply {
            if (trimmed.isEmpty()) remove(KEY_NAME) else putString(KEY_NAME, trimmed)
        }.apply()
    }

    fun info(context: Context) = PlayLibraryItemDeviceInfo(
        deviceId = id(context),
        clientName = context.getString(R.string.app_name),
        clientVersion = BuildConfig.VERSION_NAME,
        manufacturer = Build.MANUFACTURER,
        // Audiobookshelf shows an Android client as "manufacturer model" and
        // overwrites deviceName with the same, so a name is seen only here.
        model = name(context),
        sdkVersion = Build.VERSION.SDK_INT
    )

    private fun id(context: Context): String {
        val prefs = prefs(context)
        prefs.getString(KEY_ID, null)?.let { return it }
        return UUID.randomUUID().toString().also { prefs.edit().putString(KEY_ID, it).apply() }
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** As if this were another install. */
    internal fun forgetForTest(context: Context) {
        prefs(context).edit().clear().commit()
    }
}
