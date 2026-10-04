package com.paulohenriquesg.fahrenheit.api

import com.google.gson.Gson
import com.paulohenriquesg.fahrenheit.player.LatestSession
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

        assertEquals(LatestSession("phone-1", updatedAt = 3000), reply.latest())
    }

    @Test
    fun `no sessions, no device`() {
        assertNull(parse("""{"total": 0, "sessions": []}""").latest())
    }

    @Test
    fun `a session that names no device names none`() {
        assertNull(parse("""{"sessions": [{"id": "s1", "updatedAt": 1000}]}""").latest())
    }

    // Without a time it cannot excuse anything: the check then asks.
    @Test
    fun `a session that says no time is no latest session`() {
        assertNull(parse("""{"sessions": [{"id": "s1", "deviceInfo": {"deviceId": "tv-1"}}]}""").latest())
    }

    // #158: the question says where it was heard: "on your iPhone".
    @Test
    fun `the latest session names its device`() {
        val reply = parse(
            """
            {"sessions": [
              {"id": "s1", "updatedAt": 3000, "deviceInfo": {"deviceId": "phone-1", "deviceName": "iPhone", "clientName": "Abs iOS"}}
            ]}
            """
        )

        assertEquals("iPhone", reply.latest()!!.deviceName)
    }

    // A browser sends no device name; its client is the closest thing to one.
    @Test
    fun `without a device name, the client names it`() {
        val reply = parse(
            """{"sessions": [{"updatedAt": 3000, "deviceInfo": {"deviceId": "web-1", "deviceName": " ", "clientName": "Abs Web"}}]}"""
        )

        assertEquals("Abs Web", reply.latest()!!.deviceName)
    }

    @Test
    fun `a device with no name has none`() {
        val reply = parse("""{"sessions": [{"updatedAt": 3000, "deviceInfo": {"deviceId": "web-1"}}]}""")

        assertNull(reply.latest()!!.deviceName)
    }
}
