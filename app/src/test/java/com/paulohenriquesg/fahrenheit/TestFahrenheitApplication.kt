package com.paulohenriquesg.fahrenheit

/**
 * The app under unit test: as in use, but without the listening screensaver
 * on every screen (#156). Its controller and timer belong in the tests that
 * are about it, not in every screen's.
 */
class TestFahrenheitApplication : FahrenheitApplication() {
    override val listeningScreensaver: Boolean = false
}
