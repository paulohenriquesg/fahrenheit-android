package com.paulohenriquesg.fahrenheit.player

import androidx.media3.test.utils.FakeMediaSourceFactory
import androidx.media3.test.utils.FakeTimeline

/**
 * Fake media where every file is an hour long, whatever its URI. The default
 * fake file is 10 seconds, and a start position past that is clamped, so a
 * book resumed 15 minutes into a file would silently start elsewhere.
 */
fun hourLongFiles() = FakeMediaSourceFactory(
    FakeTimeline.TimelineWindowDefinition.Builder().setDurationUs(3_600_000_000L)
)
