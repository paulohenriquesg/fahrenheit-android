package com.paulohenriquesg.fahrenheit.player

import com.paulohenriquesg.fahrenheit.api.ApiService
import com.paulohenriquesg.fahrenheit.api.MediaProgressRequest
import com.paulohenriquesg.fahrenheit.api.PlayLibraryItemDeviceInfo
import com.paulohenriquesg.fahrenheit.api.PlayLibraryItemRequest
import com.paulohenriquesg.fahrenheit.api.SessionSyncRequest
import retrofit2.awaitResponse

/**
 * One item's listening, reported as Audiobookshelf's own clients do: a
 * playback session, opened when there is first something to report, synced
 * with the position and the seconds listened, closed at a stop (#92).
 *
 * A sync saves the position too, so this is the one writer of it. The
 * progress PATCH is only a fallback, for when no session can be had: the
 * position is still saved, and only that round's listening time is lost.
 *
 * Sessions live in the server's memory, and a restart or 36 idle hours drops
 * them; a 404 opens a new one, once.
 *
 * @param api the current client; null when signed out.
 */
class ListeningSession(
    private val file: QueuedFile,
    private val api: () -> ApiService?,
    private val device: PlayLibraryItemDeviceInfo
) : ListeningDelivery {

    private var id: String? = null

    override suspend fun sync(report: ListeningReport) = deliver(report, closing = false)

    override suspend fun close(report: ListeningReport?) {
        if (report != null) return deliver(report, closing = true)
        val open = id ?: return
        id = null
        service().closeSession(open, SessionSyncRequest()).awaitResponse()
    }

    private suspend fun deliver(report: ListeningReport, closing: Boolean) {
        val body = SessionSyncRequest(report.currentTime, report.timeListened, report.duration)
        repeat(2) {
            val session = id ?: open() ?: return patch(report)
            val call = if (closing) service().closeSession(session, body) else service().syncSession(session, body)
            val response = call.awaitResponse()
            if (response.isSuccessful) {
                if (closing) id = null
                return
            }
            if (response.code() != 404) error("session rejected: ${response.code()}")
            // The server forgot the session (restart, or idle too long).
            id = null
        }
        patch(report)
    }

    /** The new session's id, or null when the server would not open one. */
    private suspend fun open(): String? {
        val request = PlayLibraryItemRequest(
            deviceInfo = device,
            // The app plays the files itself; without this the server may
            // set up a transcode nobody listens to.
            forceDirectPlay = true,
            supportedMimeTypes = SUPPORTED_MIME_TYPES,
            mediaPlayer = "ExoPlayer"
        )
        val call = file.episodeId?.let { service().playLibraryItem(file.itemId, it, request) }
            ?: service().playBook(file.itemId, request)
        val response = runCatching { call.awaitResponse() }.getOrNull() ?: return null
        return response.body()?.id.takeIf { response.isSuccessful }?.also { id = it }
    }

    private suspend fun patch(report: ListeningReport) {
        val request = MediaProgressRequest(currentTime = report.currentTime, duration = report.duration)
        val call = file.episodeId?.let { service().userCreateOrUpdateMediaProgress(file.itemId, it, request) }
            ?: service().userCreateOrUpdateMediaProgress(file.itemId, request)
        val response = call.awaitResponse()
        if (!response.isSuccessful) error("progress rejected: ${response.code()}")
    }

    private fun service(): ApiService = api() ?: error("signed out")

    private companion object {
        val SUPPORTED_MIME_TYPES = listOf(
            "audio/mpeg", "audio/mp4", "audio/x-m4b", "audio/aac", "audio/flac", "audio/ogg", "audio/opus", "audio/wav", "audio/webm"
        )
    }
}
