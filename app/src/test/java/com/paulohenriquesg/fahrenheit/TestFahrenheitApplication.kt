package com.paulohenriquesg.fahrenheit

import com.paulohenriquesg.fahrenheit.progress.ProgressStore

/**
 * The app under unit test: as in use, but without the listening screensaver
 * on every screen (#156), nor the progress read on coming back (#207). Their
 * controllers, timers and requests belong in the tests that are about them,
 * not in every screen's.
 */
class TestFahrenheitApplication : FahrenheitApplication() {
    override val listeningScreensaver: Boolean = false
    override val progressResync: Boolean = false

    override fun onCreate() {
        super.onCreate()
        // The store is the process's, and outlives each test's application.
        ProgressStore.process.clear()
    }
}
