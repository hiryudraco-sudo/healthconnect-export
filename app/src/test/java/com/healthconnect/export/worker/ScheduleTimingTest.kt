package com.healthconnect.export.worker

import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class ScheduleTimingTest {
    private val paris = ZoneId.of("Europe/Paris")

    @Test
    fun `at 04 08 target 05 must be 52 minutes later`() {
        val now = ZonedDateTime.of(2026, 11, 26, 4, 8, 0, 0, paris)
        assertEquals(52L * 60_000L, ScheduleTiming.nextDelayMillis(now, 5))
    }

    @Test
    fun `mid-minute scheduling never runs early`() {
        val now = ZonedDateTime.of(2026, 11, 26, 4, 8, 59, 500_000_000, paris)
        assertEquals(51L * 60_000L + 500L, ScheduleTiming.nextDelayMillis(now, 5))
    }

    @Test
    fun `after target schedule next day`() {
        val now = ZonedDateTime.of(2026, 11, 26, 5, 0, 0, 0, paris)
        assertEquals(24L * 3_600_000L, ScheduleTiming.nextDelayMillis(now, 5))
    }

    @Test
    fun `spring DST transition uses next actual local hour`() {
        val now = ZonedDateTime.of(2027, 3, 28, 1, 30, 0, 0, paris)
        // 02:00 does not exist: it resolves to 03:00 CEST, only 30 elapsed minutes later.
        assertEquals(30L * 60_000L, ScheduleTiming.nextDelayMillis(now, 2))
    }

    @Test
    fun `autumn DST transition respects elapsed duration`() {
        val now = ZonedDateTime.of(2027, 10, 31, 1, 30, 0, 0, paris)
        // 05:00 CET is 4h30 elapsed hours from 01:30 CEST.
        assertEquals(4L * 3_600_000L + 30L * 60_000L, ScheduleTiming.nextDelayMillis(now, 5))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invalid schedule hour is rejected`() {
        ScheduleTiming.nextDelayMillis(ZonedDateTime.now(paris), 25)
    }
}
