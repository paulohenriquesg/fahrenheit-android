package com.paulohenriquesg.fahrenheit.login

import com.paulohenriquesg.fahrenheit.api.ServerStatus
import java.net.InetAddress

/** An Audiobookshelf server that answered on this network. */
data class FoundServer(val address: String, val version: String?)

class ServerScan(
    private val probe: suspend (baseUrl: String) -> ServerStatus?,
    private val parallel: Int = 32,
    private val perHostMillis: Long = 1_500
) {
    suspend fun scan(ownAddress: String, onFound: (FoundServer) -> Unit) {}

    companion object {
        fun candidates(ownAddress: String): List<String> = emptyList()
    }
}

object LocalNetwork {
    fun pick(addresses: List<InetAddress>): String? = null
}
