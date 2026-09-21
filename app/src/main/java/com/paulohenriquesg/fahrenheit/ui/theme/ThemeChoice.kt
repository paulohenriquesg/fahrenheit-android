package com.paulohenriquesg.fahrenheit.ui.theme

object ThemeChoice {

    /**
     * Dark or light: what the person picked in settings, or failing that what
     * the device is set to. The app used to default to light and ignore the
     * device entirely, so a TV in dark mode opened a white app.
     */
    fun resolve(chosen: Boolean?, systemIsDark: Boolean): Boolean = chosen ?: systemIsDark
}
