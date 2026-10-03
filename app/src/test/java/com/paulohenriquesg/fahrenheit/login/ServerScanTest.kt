package com.paulohenriquesg.fahrenheit.login

import com.paulohenriquesg.fahrenheit.api.ServerStatus
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

        assertTrue("${most.get()} at once", most.get() <= 8)
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

    // Never a public range: the scan is for the home network only.
    @Test
    fun `the device's own address is its private IPv4 one`() {
        val picked = LocalNetwork.pick(
            listOf(
                InetAddress.getByName("::1"),
                InetAddress.getByName("127.0.0.1"),
                InetAddress.getByName("fe80::1"),
                InetAddress.getByName("192.168.1.37")
            )
        )

        assertEquals("192.168.1.37", picked)
    }

    @Test
    fun `a public address is never scanned around`() {
        assertEquals(null, LocalNetwork.pick(listOf(InetAddress.getByName("203.0.113.7"))))
    }
}
