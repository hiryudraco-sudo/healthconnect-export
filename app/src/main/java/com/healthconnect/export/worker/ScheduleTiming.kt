package com.healthconnect.export.worker

import java.time.Duration
import java.time.ZonedDateTime

/**
 * Computes the next local wall-clock occurrence of a configured hour.
 * Uses a complete instant duration (not truncated minutes), so 05:00 can never
 * be scheduled at 04:59 just because scheduling started mid-minute.
 *
 * DST gaps resolve to the next valid local time; overlaps use ZonedDateTime's
 * first valid offset. WorkManager's subsequent periodic intervals are elapsed
 * 24-hour periods, not guaranteed wall-clock recurrences across DST.
 */
internal object ScheduleTiming {
    fun nextDelayMillis(now: ZonedDateTime, hour: Int): Long {
        require(hour in 0..23)
        var next = now.toLocalDate().atTime(hour, 0).atZone(now.zone)
        if (!next.toInstant().isAfter(now.toInstant())) {
            next = now.toLocalDate().plusDays(1).atTime(hour, 0).atZone(now.zone)
        }
        return Duration.between(now.toInstant(), next.toInstant()).toMillis().coerceAtLeast(1L)
    }
}
