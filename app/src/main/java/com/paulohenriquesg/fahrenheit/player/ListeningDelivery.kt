package com.paulohenriquesg.fahrenheit.player

/** Where one item's listening reports go. */
interface ListeningDelivery {
    /** Throws when the report could not be delivered, so it is retried. */
    suspend fun sync(report: ListeningReport)

    /** Ends this stretch of listening, with a last report if there is one. */
    suspend fun close(report: ListeningReport?)
}
