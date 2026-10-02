package com.paulohenriquesg.fahrenheit.api

/**
 * What the app needs from a newly opened playback session: its id.
 *
 * The server answers with the whole session, library item included, and the
 * item's shape varies by book - an ebook's file is an object, for one. Reading
 * only the id keeps opening a session independent of all that (#92).
 */
data class OpenedSession(val id: String)
