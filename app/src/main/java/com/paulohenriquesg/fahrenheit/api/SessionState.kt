package com.paulohenriquesg.fahrenheit.api

/** What [ApiClient.initialize] found, so the caller can decide what to show. */
enum class SessionState {
    /** Stored credentials are usable; [ApiClient.getApiService] will serve. */
    Ready,

    /** Nothing usable was stored. The stale credentials have been cleared. */
    NeedsLogin
}
