package com.paulohenriquesg.fahrenheit.podcast

import com.paulohenriquesg.fahrenheit.auth.SessionRole
import org.junit.Assert.assertEquals
import org.junit.Test

/** Which of an admin's buttons the podcast page keeps a place for before it knows the item (#225). */
class PodcastSlotsTest {

    @Test
    fun `an admin or root keeps the feed check's place, and anyone who may update the downloads'`() {
        assertEquals(PodcastSlots(feedCheck = true, downloads = true), PodcastSlots.of(SessionRole("admin", mayUpdate = true)))
        assertEquals(PodcastSlots(feedCheck = true, downloads = false), PodcastSlots.of(SessionRole("root", mayUpdate = false)))
        assertEquals(PodcastSlots(feedCheck = false, downloads = true), PodcastSlots.of(SessionRole("user", mayUpdate = true)))
    }

    @Test
    fun `a listener, or no stored role, keeps none`() {
        assertEquals(PodcastSlots.NONE, PodcastSlots.of(SessionRole("user", mayUpdate = false)))
        assertEquals(PodcastSlots.NONE, PodcastSlots.of(null))
    }
}
