package com.paulohenriquesg.fahrenheit.player

import org.junit.Assert.assertEquals
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

    // #107: skip lengths, 10/15/30/60 s, 30 and 30 by default.
    @Test fun `skips are 30 seconds each way until changed`() {
        assertEquals(30, PlayerSettings(context).skipBackSeconds)
        assertEquals(30, PlayerSettings(context).skipForwardSeconds)
    }

    @Test fun `a chosen skip length stays`() {
        PlayerSettings(context).skipBackSeconds = 10
        PlayerSettings(context).skipForwardSeconds = 60
        assertEquals(10, PlayerSettings(context).skipBackSeconds)
        assertEquals(60, PlayerSettings(context).skipForwardSeconds)
    }

    // Review Focus 2.
    @Test fun `a length not on offer reads as 30`() {
        PlayerSettings(context).skipBackSeconds = 7
        assertEquals(30, PlayerSettings(context).skipBackSeconds)
    }

    @Test fun `the lengths on offer`() = assertEquals(listOf(10, 15, 30, 60), PlayerSettings.SKIP_LENGTHS)
}
