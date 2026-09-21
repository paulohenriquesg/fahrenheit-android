package com.paulohenriquesg.fahrenheit

import org.junit.After
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * One MediaPlayer for the whole app: a device has a limited number of codecs,
 * and a player left unreleased keeps one, plus audio focus, for as long as the
 * process lives.
 */
@RunWith(RobolectricTestRunner::class)
class GlobalMediaPlayerTest {

    @After
    fun tearDown() = GlobalMediaPlayer.release()

    @Test
    fun `the same player serves every screen`() =
        assertSame(GlobalMediaPlayer.getInstance(), GlobalMediaPlayer.getInstance())

    // Anything done to a released player throws, so a fresh one has to follow.
    @Test
    fun `a released player is never handed out again`() {
        val before = GlobalMediaPlayer.getInstance()

        GlobalMediaPlayer.release()

        assertNotSame(before, GlobalMediaPlayer.getInstance())
    }

    @Test
    fun `releasing when nothing was ever asked for is harmless`() {
        GlobalMediaPlayer.release()
        GlobalMediaPlayer.release()
    }
}
