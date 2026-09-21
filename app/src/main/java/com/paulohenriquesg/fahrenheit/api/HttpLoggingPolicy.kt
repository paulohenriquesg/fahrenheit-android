package com.paulohenriquesg.fahrenheit.api

import okhttp3.logging.HttpLoggingInterceptor

object HttpLoggingPolicy {

    /**
     * How much of each request to log.
     *
     * Nothing secret is printed at BASIC - tokens travel in headers, which it
     * leaves out - but a shipped app has no business narrating what someone is
     * listening to into the system log, where any app with log access can read
     * it. BASIC rather than BODY while developing: full bodies OOM on large
     * library responses.
     */
    fun level(debugBuild: Boolean): HttpLoggingInterceptor.Level =
        if (debugBuild) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
}
