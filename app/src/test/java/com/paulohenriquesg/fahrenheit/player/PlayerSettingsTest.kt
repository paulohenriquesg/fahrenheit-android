package com.paulohenriquesg.fahrenheit.player

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** "Play the next episode automatically", off by default (#108). */
@RunWith(AndroidJUnit4::class)
class PlayerSettingsTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test fun `off until turned on`() = assertFalse(PlayerSettings(context).playNextEpisode)

    @Test fun `turned on, it stays on`() {
        PlayerSettings(context).playNextEpisode = true
        assertTrue(PlayerSettings(context).playNextEpisode)
    }
}
