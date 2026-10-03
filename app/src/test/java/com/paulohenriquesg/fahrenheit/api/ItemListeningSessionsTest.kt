package com.paulohenriquesg.fahrenheit.api

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Progress records do not say which device wrote them; listening sessions do
 * (#90). The latest one for an item tells whether this device listened last.
 */
class ItemListeningSessionsTest {

    private fun parse(json: String) = Gson().fromJson(json, ItemListeningSessions::class.java)

    @Test
    fun `the latest session is the most recently updated, not the first listed`() {
        val reply = parse(
            """
            {"total": 2, "sessions": [
              {"id": "s1", "updatedAt": 1000, "deviceInfo": {"deviceId": "tv-1"}},
              {"id": "s2", "updatedAt": 3000, "deviceInfo": {"deviceId": "phone-1"}}
            ]}
            """
        )

        assertEquals("phone-1", reply.latestDeviceId())
    }

    @Test
    fun `no sessions, no device`() {
        assertNull(parse("""{"total": 0, "sessions": []}""").latestDeviceId())
    }

    @Test
    fun `a session that names no device names none`() {
        assertNull(parse("""{"sessions": [{"id": "s1", "updatedAt": 1000}]}""").latestDeviceId())
    }
}
