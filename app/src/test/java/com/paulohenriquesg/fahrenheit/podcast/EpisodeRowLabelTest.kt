package com.paulohenriquesg.fahrenheit.podcast

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** What the right-hand end of an episode row says. */
class EpisodeRowLabelTest {

    private fun label(state: DownloadState?, downloaded: Boolean = false, focused: Boolean = false) =
        EpisodeRowLabel.of(downloaded, state, focused)

    @Test
    fun `an episode on the server plays`() = assertEquals("Play", label(null, downloaded = true))

    @Test
    fun `a missing episode offers the download only where a press would do it`() {
        assertEquals("Not downloaded", label(null))
        assertEquals("Download to server", label(null, focused = true))
    }

    @Test
    fun `the queue position is said plainly`() {
        assertEquals("Waiting · next", label(DownloadState.Waiting(ahead = 0)))
        assertEquals("Waiting · 1 ahead", label(DownloadState.Waiting(ahead = 1)))
        assertEquals("Waiting · 3 ahead", label(DownloadState.Waiting(ahead = 3)))
    }

    @Test
    fun `the other states`() {
        assertEquals("Requested…", label(DownloadState.Requested))
        assertEquals("Downloading…", label(DownloadState.Downloading))
        assertEquals("Download failed", label(DownloadState.Failed))
    }

    @Test
    fun `only a fresh or failed episode can be asked for again`() {
        assertTrue(EpisodeRowLabel.mayDownload(null))
        assertTrue(EpisodeRowLabel.mayDownload(DownloadState.Failed))
        assertFalse(EpisodeRowLabel.mayDownload(DownloadState.Requested))
        assertFalse(EpisodeRowLabel.mayDownload(DownloadState.Waiting(0)))
        assertFalse(EpisodeRowLabel.mayDownload(DownloadState.Downloading))
    }

    @Test
    fun `a half-heard episode resumes, a heard one plays`() {
        assertEquals("Resume", EpisodeRowLabel.of(true, null, false, EpisodeProgress.InProgress(0.4, 840.0)))
        assertEquals("Play", EpisodeRowLabel.of(true, null, false, EpisodeProgress.Heard))
    }
}
