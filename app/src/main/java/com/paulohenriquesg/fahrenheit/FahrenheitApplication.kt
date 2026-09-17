package com.paulohenriquesg.fahrenheit

import android.app.Application
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
    }
}