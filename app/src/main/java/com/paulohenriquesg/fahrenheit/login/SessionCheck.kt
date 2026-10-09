package com.paulohenriquesg.fahrenheit.login

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import retrofit2.HttpException

/**
 * Whether the stored session still works, asked at launch (#191).
 *
 * @param probe any authenticated request. Through the app's client, an
 *   expired access token is refreshed on the way, so by the time this says
 *   [Result.Ready] Home's first requests carry a token the server accepts.
 * @param timeoutMs how long the launch screen waits. The client's own limits
 *   are for a library's worth of JSON (a 60 s read); a server that takes the
 *   connection and says nothing would hold the launch screen up for a minute.
 */
class SessionCheck(
    private val timeoutMs: Long = 10_000,
    private val probe: suspend () -> Unit
) {
    sealed interface Result {
        data object Ready : Result

        /** The server said no, after the refresh token was spent too. */
        data object Rejected : Result

        /**
         * Anything short of a no: no network yet, no answer in time, a server
         * failing. The session may be perfectly good; a TV just woken often
         * has no Wi-Fi for its first seconds.
         */
        data object Unreachable : Result
    }

    suspend fun run(): Result = try {
        withTimeoutOrNull(timeoutMs) { probe() }
            ?.let { Result.Ready }
            ?: Result.Unreachable
    } catch (e: CancellationException) {
        // Back, or the screen being rebuilt: no verdict on the session.
        throw e
    } catch (e: HttpException) {
        if (e.code() == 401 || e.code() == 403) Result.Rejected else Result.Unreachable
    } catch (e: Exception) {
        Result.Unreachable
    }
}
