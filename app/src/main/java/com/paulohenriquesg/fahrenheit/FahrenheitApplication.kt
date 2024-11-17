package com.paulohenriquesg.fahrenheit

import android.app.Application
import com.paulohenriquesg.fahrenheit.api.ApiClient

class FahrenheitApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ApiClient.initialize(this)
    }
}