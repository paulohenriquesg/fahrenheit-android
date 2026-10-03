package com.paulohenriquesg.fahrenheit.player

class ServerKnowledge {
    fun saw(itemId: String, episodeId: String?, at: Long) {}

    fun knownAt(itemId: String, episodeId: String?): Long? = null

    companion object {
        val process = ServerKnowledge()
    }
}
