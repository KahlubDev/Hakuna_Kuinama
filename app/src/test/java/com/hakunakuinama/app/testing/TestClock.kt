package com.hakunakuinama.app.testing

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler

/**
 * A [Clock] whose "now" follows the test scheduler's virtual time.
 *
 * Needed to test the slot-rollover ticker honestly. A `Clock.fixed` clock never changes,
 * so the ticker would emit the same slot forever and the rollover would be untestable; a
 * real system clock would make the test take an hour and still be flaky.
 *
 * Advance the scheduler and this clock advances with it — which is exactly the
 * relationship the production code has with the wall clock.
 *
 * Defaults to Nairobi time, because that is where the app's users are.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TestClock(
    private val start: ZonedDateTime,
    private val scheduler: TestCoroutineScheduler,
) : Clock() {

    override fun getZone(): ZoneId = start.zone

    override fun withZone(zone: ZoneId): Clock = TestClock(start.withZoneSameInstant(zone), scheduler)

    override fun instant(): Instant = start.toInstant().plusMillis(scheduler.currentTime)

    /** Current virtual time, for asserting on. */
    fun now(): ZonedDateTime = ZonedDateTime.ofInstant(instant(), start.zone)

    companion object {
        val NAIROBI: ZoneId = ZoneId.of("Africa/Nairobi")

        /** A clock parked at a given wall-clock time on a fixed date. */
        fun at(hour: Int, minute: Int = 0, second: Int = 0): ZonedDateTime =
            ZonedDateTime.of(
                LocalDate.of(2026, 9, 26),
                LocalTime.of(hour, minute, second),
                NAIROBI,
            )
    }
}
