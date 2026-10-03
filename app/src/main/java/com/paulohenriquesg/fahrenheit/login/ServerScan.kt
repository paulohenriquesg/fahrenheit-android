package com.paulohenriquesg.fahrenheit.login

import com.paulohenriquesg.fahrenheit.api.ServerStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeoutOrNull
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface

/** An Audiobookshelf server that answered on this network. */
data class FoundServer(val address: String, val version: String?)

/**
 * Finds Audiobookshelf servers on the local network (#101): an address is two
 * dozen characters of punctuation on a D-pad, so the first run offers what
 * answers instead of asking for it.
 *
 * Asks every address in this device's /24 for `/status` on Audiobookshelf's
 * port, a few at a time, and reports the ones that say they are
 * Audiobookshelf as they answer.
 *
 * @param probe fetches `/status` from a base URL; null or a throw is no server.
 * @param parallel how many addresses are asked at once.
 * @param perHostMillis how long one address gets before it counts as silent.
 */
class ServerScan(
    private val probe: suspend (baseUrl: String) -> ServerStatus?,
    private val parallel: Int = 32,
    private val perHostMillis: Long = 1_500
) {
    suspend fun scan(ownAddress: String, onFound: (FoundServer) -> Unit) = coroutineScope {
        val permits = Semaphore(parallel)
        for (url in candidates(ownAddress)) {
            launch {
                val status = permits.withPermit {
                    withTimeoutOrNull(perHostMillis) { askQuietly(url) }
                }
                if (status?.app.equals(APP, ignoreCase = true)) {
                    onFound(FoundServer(url, status?.serverVersion))
                }
            }
        }
    }

    // Refused, reset, not HTTP, not JSON: all just "no server here". A
    // cancellation still goes through, so a timeout or a closed screen ends it.
    private suspend fun askQuietly(url: String): ServerStatus? = try {
        probe(url)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    companion object {
        /** Audiobookshelf's default port; a server moved elsewhere is typed in. */
        const val PORT = 13378
        private const val APP = "audiobookshelf"

        /**
         * Every host address in the /24 around [ownAddress]. A /24 even on a
         * wider network: 254 probes take seconds, a /16's 65,534 do not.
         */
        fun candidates(ownAddress: String): List<String> {
            val prefix = ownAddress.substringBeforeLast('.')
            return (1..254).map { "http://$prefix.$it:$PORT" }
        }
    }
}

/** Where this device sits on the local network. */
object LocalNetwork {
    /** This device's private IPv4 address, if it is on a local network at all. */
    fun ownAddress(): String? = runCatching {
        NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
            .filter { it.isUp && !it.isLoopback }
            .flatMap { it.inetAddresses.toList() }
    }.getOrNull()?.let(::pick)

    /**
     * The first private (RFC 1918) IPv4 address. Never a public one: the scan
     * is for the home network, not someone else's.
     */
    fun pick(addresses: List<InetAddress>): String? =
        addresses.filterIsInstance<Inet4Address>()
            .firstOrNull { it.isSiteLocalAddress }
            ?.hostAddress
}
