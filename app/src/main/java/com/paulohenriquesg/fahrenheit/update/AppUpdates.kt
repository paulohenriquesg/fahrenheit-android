package com.paulohenriquesg.fahrenheit.update

import android.content.Context
import android.os.Build
import com.paulohenriquesg.fahrenheit.BuildConfig
import com.paulohenriquesg.fahrenheit.storage.SharedPreferencesHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * The app's update checks: everything context-bound that the decision layer
 * ([ManifestUpdateChecker]) deliberately knows nothing about.
 */
object AppUpdates {

    /**
     * Published beside each release APK. The "latest" release is rewritten on
     * every release, so this URL never changes.
     */
    const val MANIFEST_URL =
        "https://github.com/paulohenriquesg/fahrenheit-android/releases/download/latest/update-manifest.json"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    fun checker(context: Context): ManifestUpdateChecker {
        val app = context.applicationContext
        return ManifestUpdateChecker(
            fetchManifest = { fetch(MANIFEST_URL) },
            installedVersionCode = { BuildConfig.VERSION_CODE },
            language = { deviceLanguage(app) },
            snoozeStore = PreferencesSnoozeStore(app),
            now = System::currentTimeMillis
        )
    }

    fun isEnabled(context: Context): Boolean =
        SharedPreferencesHandler(context).getUserPreferences().updateCheckEnabled

    /** Notes the version handed to the installer, to check next launch that it landed. */
    fun recordPendingInstall(context: Context, versionCode: Int) {
        val handler = SharedPreferencesHandler(context)
        handler.saveUserPreferences(
            handler.getUserPreferences().copy(pendingInstallVersionCode = versionCode)
        )
    }

    /**
     * What became of the last install we dispatched. Clears the note, so each
     * dispatch is judged once.
     */
    fun takePendingInstallOutcome(context: Context): PendingInstall.Outcome {
        val handler = SharedPreferencesHandler(context)
        val preferences = handler.getUserPreferences()
        val outcome = PendingInstall.outcome(
            pendingVersionCode = preferences.pendingInstallVersionCode,
            installedVersionCode = BuildConfig.VERSION_CODE
        )
        if (outcome != PendingInstall.Outcome.Nothing) {
            handler.saveUserPreferences(preferences.copy(pendingInstallVersionCode = null))
        }
        return outcome
    }

    private suspend fun fetch(url: String): String? = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).header("Cache-Control", "no-cache").build()
        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) response.body.string() else null
        }
    }

    private fun deviceLanguage(context: Context): String {
        val locales = context.resources.configuration.locales
        val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !locales.isEmpty) {
            locales.get(0)
        } else {
            @Suppress("DEPRECATION") context.resources.configuration.locale
        }
        return locale?.toLanguageTag() ?: "en"
    }
}
