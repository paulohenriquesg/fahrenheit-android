package com.paulohenriquesg.fahrenheit.login

import com.paulohenriquesg.fahrenheit.api.ApiClient
import com.paulohenriquesg.fahrenheit.api.ServerStatus
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.InetAddress
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger

/**
 * Finding the server instead of typing it (#101, frame 1 of the login mock):
 * an address is two dozen characters of punctuation on a D-pad. The scan asks
 * every address in this device's /24 for Audiobookshelf's status on its port.
 */
class ServerScanTest {

    @Test
    fun `the candidates are this device's slash 24, on Audiobookshelf's port`() {
        val candidates = ServerScan.candidates("192.168.1.37")

        assertEquals(254, candidates.size)
        assertEquals("http://192.168.1.1:13378", candidates.first())
        assertEquals("http://192.168.1.254:13378", candidates.last())
    }

    @Test
    fun `only servers that say they are Audiobookshelf are found`() = runBlocking {
        val scan = ServerScan(probe = { url ->
            when (url) {
                "http://10.0.0.2:13378" -> ServerStatus(app = "audiobookshelf", serverVersion = "2.17.0")
                "http://10.0.0.3:13378" -> ServerStatus(app = "something-else")
                "http://10.0.0.4:13378" -> throw IOException("connection refused")
                else -> null
            }
        })
        val found = mutableListOf<FoundServer>()

        scan.scan("10.0.0.9") { found += it }

        assertEquals(listOf(FoundServer("http://10.0.0.2:13378", "2.17.0")), found)
    }

    // 254 sockets at once is a burst a TV's wifi and a home router notice.
    @Test
    fun `no more than the allowed number of probes run at once, and every address is asked`() = runBlocking {
        val inFlight = AtomicInteger()
        val most = AtomicInteger()
        val asked = Collections.synchronizedSet(mutableSetOf<String>())
        val scan = ServerScan(parallel = 8, probe = { url ->
            most.accumulateAndGet(inFlight.incrementAndGet(), ::maxOf)
            delay(5)
            asked += url
            inFlight.decrementAndGet()
            null
        })

        scan.scan("10.0.0.9") {}

        assertEquals("at once", 8, most.get())
        assertEquals(254, asked.size)
    }

    @Test
    fun `an address that never answers does not hold up the scan`() = runBlocking {
        val scan = ServerScan(perHostMillis = 50, probe = { url ->
            if (url == "http://10.0.0.5:13378") awaitCancellation()
            if (url == "http://10.0.0.6:13378") ServerStatus(app = "audiobookshelf") else null
        })
        val found = mutableListOf<FoundServer>()

        scan.scan("10.0.0.9") { found += it }

        assertEquals(listOf("http://10.0.0.6:13378"), found.map { it.address })
    }

    // A probe waiting for its turn has not started; only the asking is timed.
    @Test
    fun `waiting for a turn does not count against an address's time`() = runBlocking {
        val scan = ServerScan(parallel = 1, perHostMillis = 50, probe = { url ->
            delay(20)
            if (url == "http://10.0.0.254:13378") ServerStatus(app = "audiobookshelf") else null
        })
        val found = mutableListOf<FoundServer>()

        scan.scan("10.0.0.9") { found += it }

        assertEquals(listOf("http://10.0.0.254:13378"), found.map { it.address })
    }

    // A VPN's subnet may be someone else's machines; the home network is the
    // one the TV is on.
    @Test
    fun `a VPN's address is not the home network`() {
        val picked = LocalNetwork.pick(
            listOf(
                "tun0" to InetAddress.getByName("10.8.0.2"),
                "wlan0" to InetAddress.getByName("192.168.1.37")
            )
        )

        assertEquals("192.168.1.37", picked)
    }

    // Never a public range: the scan is for the home network only.
    @Test
    fun `the device's own address is its private IPv4 one`() {
        val picked = LocalNetwork.pick(
            listOf(
                "lo" to InetAddress.getByName("::1"),
                "lo" to InetAddress.getByName("127.0.0.1"),
                "wlan0" to InetAddress.getByName("fe80::1"),
                "wlan0" to InetAddress.getByName("192.168.1.37")
            )
        )

        assertEquals("192.168.1.37", picked)
    }

    @Test
    fun `a public address is never scanned around`() {
        assertEquals(null, LocalNetwork.pick(listOf("eth0" to InetAddress.getByName("203.0.113.7"))))
    }

    // Whatever answers on the port could otherwise send the probe anywhere,
    // the internet included.
    @Test
    fun `a probe does not follow a redirect`() {
        val elsewhere = MockWebServer().apply {
            enqueue(MockResponse().setBody("{\"app\":\"audiobookshelf\"}"))
            start()
        }
        val local = MockWebServer().apply {
            enqueue(MockResponse().setResponseCode(302).setHeader("Location", elsewhere.url("/status")))
            start()
        }

        val status = runCatching {
            runBlocking { ApiClient.createProbeApi(local.url("/").toString()).status() }
        }.getOrNull()

        assertEquals(null, status?.app)
        assertEquals(0, elsewhere.requestCount)
        local.shutdown()
        elsewhere.shutdown()
    }
}
