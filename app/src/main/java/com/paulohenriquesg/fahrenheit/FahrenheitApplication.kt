package com.paulohenriquesg.fahrenheit

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.paulohenriquesg.fahrenheit.player.AppVisibility
import com.paulohenriquesg.fahrenheit.api.ApiClient
import com.paulohenriquesg.fahrenheit.ui.theme.LayoutManager
import com.paulohenriquesg.fahrenheit.ui.theme.ThemeManager

class FahrenheitApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Result intentionally ignored here: Application.onCreate must not
        // navigate. LoginActivity/MainActivity decide what to show.
        ApiClient.initialize(this)
        ThemeManager.initialize(this)
        LayoutManager.initialize(this)
        // Whether a screen shows: the playback service opens the player only then (#144).
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) = AppVisibility.process.started()
            override fun onActivityStopped(activity: Activity) = AppVisibility.process.stopped()
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }
}