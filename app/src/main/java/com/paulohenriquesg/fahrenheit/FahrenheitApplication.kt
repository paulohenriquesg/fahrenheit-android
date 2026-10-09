package com.paulohenriquesg.fahrenheit

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.paulohenriquesg.fahrenheit.player.AppVisibility
import com.paulohenriquesg.fahrenheit.player.PlayerActivity
import com.paulohenriquesg.fahrenheit.api.ApiClient
import com.paulohenriquesg.fahrenheit.progress.ProgressResync
import com.paulohenriquesg.fahrenheit.screensaver.installListeningScreensaver
import com.paulohenriquesg.fahrenheit.ui.theme.LayoutManager
import com.paulohenriquesg.fahrenheit.ui.theme.ThemeManager
import com.paulohenriquesg.fahrenheit.utils.StartupTimeline

open class FahrenheitApplication : Application() {
    /** Our own screensaver while listening, on every screen (#156); off under unit test. */
    protected open val listeningScreensaver: Boolean = true

    /** Reading the server's progress on coming back (#207); off under unit test, where it would be one more request to every fake server. */
    protected open val progressResync: Boolean = true

    override fun onCreate() {
        super.onCreate()
        // Result intentionally ignored here: Application.onCreate must not
        // navigate. LoginActivity/MainActivity decide what to show.
        ApiClient.initialize(this)
        ThemeManager.initialize(this)
        LayoutManager.initialize(this)
        StartupTimeline.app.mark("app created")
        // Whether a screen shows: the playback service opens the player only then (#144).
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) {
                val cameBack = AppVisibility.process.started(player = activity is PlayerActivity)
                // Listening done elsewhere meanwhile shows on every screen (#207).
                if (cameBack && progressResync) ProgressResync.process.request()
                // Our own screensaver while something plays, on every screen (#156).
                if (listeningScreensaver) installListeningScreensaver(activity)
            }
            override fun onActivityStopped(activity: Activity) = AppVisibility.process.stopped(player = activity is PlayerActivity)
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }
}