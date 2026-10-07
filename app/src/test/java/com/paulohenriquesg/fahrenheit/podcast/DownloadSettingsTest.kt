package com.paulohenriquesg.fahrenheit.podcast

import com.paulohenriquesg.fahrenheit.TestFixtures
import com.paulohenriquesg.fahrenheit.api.Me
import com.paulohenriquesg.fahrenheit.api.MePermissions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The show's auto-download settings as the server keeps them (#182). */
class DownloadSettingsTest {

    private val media = TestFixtures.createMockLibraryItem().media

    @Test
    fun `read from the podcast's media`() {
        val settings = DownloadSettings.of(
            media.copy(autoDownloadEpisodes = true, autoDownloadSchedule = "0 0 * * *", maxEpisodesToKeep = 5, maxNewEpisodesToDownload = 3)
        )

        assertEquals(DownloadSettings(enabled = true, schedule = "0 0 * * *", keep = 5, perCheck = 3), settings)
    }

    // The server's defaults: off, keep all, three per check.
    @Test
    fun `what the server leaves out reads as its defaults`() {
        assertEquals(DownloadSettings(enabled = false, schedule = null, keep = 0, perCheck = 3), DownloadSettings.of(media))
    }

    @Test
    fun `the options, with the server's own value when it is not one of them`() {
        assertEquals(listOf(0, 5, 10, 25), DownloadSettings.keepOptions(5))
        assertEquals(listOf(0, 5, 7, 10, 25), DownloadSettings.keepOptions(7))
        assertEquals(listOf(1, 3, 10, 0), DownloadSettings.perCheckOptions(3))
        assertEquals(listOf(1, 3, 5, 10, 0), DownloadSettings.perCheckOptions(5))
    }

    @Test
    fun `the labels`() {
        assertEquals("All", DownloadSettings.keepLabel(0))
        assertEquals("Latest 5", DownloadSettings.keepLabel(5))
        assertEquals("All", DownloadSettings.perCheckLabel(0))
        assertEquals("3", DownloadSettings.perCheckLabel(3))
    }

    // The server answers 403 to a change from anyone without update rights.
    @Test
    fun `only a user the server lets update may change them`() {
        assertTrue(DownloadSettings.mayChange(Me(type = "admin")))
        assertTrue(DownloadSettings.mayChange(Me(type = "root")))
        assertTrue(DownloadSettings.mayChange(Me(type = "user", permissions = MePermissions(update = true))))
        assertFalse(DownloadSettings.mayChange(Me(type = "user", permissions = MePermissions(update = false))))
        assertFalse(DownloadSettings.mayChange(Me(type = "user")))
        assertFalse(DownloadSettings.mayChange(Me(type = "guest")))
        assertFalse(DownloadSettings.mayChange(null))
    }
}
