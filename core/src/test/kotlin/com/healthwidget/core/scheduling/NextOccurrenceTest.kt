package com.healthwidget.core.scheduling

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

private val NEW_YORK: ZoneId = ZoneId.of("America/New_York")
private val TOKYO: ZoneId = ZoneId.of("Asia/Tokyo")

/** [instant] is expressed as a UTC wall-clock string purely so each test case is self-evident;
 * the [zone] is what actually determines the local time the fixed clock reports. */
private fun fixedClock(
    instant: String,
    zone: ZoneId = NEW_YORK,
): Clock = Clock.fixed(Instant.parse(instant), zone)

class NextOccurrenceTest {
    @Test
    fun `target later today returns the same-day delay`() {
        // 09:00 EST -> 14:00 EST target, same day.
        val clock = fixedClock("2026-01-15T14:00:00Z", NEW_YORK) // 09:00 local (UTC-5)
        val target = LocalTime.of(14, 0)

        assertThat(durationUntilNext(target, clock)).isEqualTo(Duration.ofHours(5))
    }

    @Test
    fun `target already passed today rolls over to tomorrow`() {
        // 14:00 EST local, target 09:00 already passed today -> next occurrence is tomorrow.
        val clock = fixedClock("2026-01-15T19:00:00Z", NEW_YORK) // 14:00 local (UTC-5)
        val target = LocalTime.of(9, 0)

        assertThat(durationUntilNext(target, clock)).isEqualTo(Duration.ofHours(19))
    }

    @Test
    fun `target equal to now rolls over to a full day later`() {
        val clock = fixedClock("2026-01-15T15:30:00Z", NEW_YORK) // 10:30 local (UTC-5)
        val time = LocalTime.of(10, 30)

        assertThat(durationUntilNext(time, clock)).isEqualTo(Duration.ofHours(24))
    }

    @Test
    fun `handles times crossing midnight`() {
        val clock = fixedClock("2026-01-16T04:45:00Z", NEW_YORK) // 23:45 local on Jan 15 (UTC-5)
        val target = LocalTime.of(0, 15)

        assertThat(durationUntilNext(target, clock)).isEqualTo(Duration.ofMinutes(30))
    }

    @Test
    fun `spring-forward gap pushes a skipped local time forward by the gap length`() {
        // America/New_York, 2026-03-08: clocks jump 02:00 -> 03:00 (EST UTC-5 -> EDT UTC-4).
        // "now" is 01:30 EST, before the transition; target 02:30 never happens that day.
        val clock = fixedClock("2026-03-08T06:30:00Z", NEW_YORK) // 01:30 EST (UTC-5)
        val target = LocalTime.of(2, 30)

        // The skipped 02:30 resolves forward to 03:30 EDT (UTC-4), one hour after "now".
        assertThat(durationUntilNext(target, clock)).isEqualTo(Duration.ofHours(1))
    }

    @Test
    fun `spring-forward does not shrink the delay to a target still later that same day`() {
        // Same transition day, but "now" (00:00 EST) is well before a target (04:00) that lands
        // safely after the gap — the missing hour must still be reflected in the delay.
        val clock = fixedClock("2026-03-08T05:00:00Z", NEW_YORK) // 00:00 EST (UTC-5)
        val target = LocalTime.of(4, 0)

        // Without the missing hour this would be 4h; the spring-forward transition makes the
        // 00:00-04:00 span only 3 wall-clock hours long in elapsed real time.
        assertThat(durationUntilNext(target, clock)).isEqualTo(Duration.ofHours(3))
    }

    @Test
    fun `autumn fallback overlap resolves to its earlier occurrence`() {
        // America/New_York, 2026-11-01: clocks fall back 02:00 EDT -> 01:00 EST, so 01:00-02:00
        // happens twice. "now" is 00:30 EDT, before the transition; target is the ambiguous 01:30.
        val clock = fixedClock("2026-11-01T04:30:00Z", NEW_YORK) // 00:30 EDT (UTC-4)
        val target = LocalTime.of(1, 30)

        // Resolves to the earlier (pre-fallback, still-EDT) 01:30, one hour after "now".
        assertThat(durationUntilNext(target, clock)).isEqualTo(Duration.ofHours(1))
    }

    @Test
    fun `autumn fallback adds the repeated hour when the target is later that day`() {
        // Same transition day, "now" (00:00 EDT) targeting 03:00 (safely after both passes of
        // the repeated hour) must reflect the extra hour the fallback adds to elapsed real time.
        val clock = fixedClock("2026-11-01T04:00:00Z", NEW_YORK) // 00:00 EDT (UTC-4)
        val target = LocalTime.of(3, 0)

        // Without the repeated hour this would be 3h; the fallback makes the 00:00-03:00 span
        // 4 wall-clock hours long in elapsed real time.
        assertThat(durationUntilNext(target, clock)).isEqualTo(Duration.ofHours(4))
    }

    @Test
    fun `same instant and target yield different delays in different time zones`() {
        // The same physical moment, interpreted in two different zones, targeting the same
        // local clock time (09:00) — confirms the zone carried by Clock actually drives the
        // computation rather than any ambient/default zone.
        val instant = "2026-06-01T00:00:00Z" // 20:00 the previous day in New York, 09:00 in Tokyo
        val target = LocalTime.of(9, 0)

        val nyDelay = durationUntilNext(target, fixedClock(instant, NEW_YORK))
        val tokyoDelay = durationUntilNext(target, fixedClock(instant, TOKYO))

        assertThat(nyDelay).isEqualTo(Duration.ofHours(13))
        assertThat(tokyoDelay).isEqualTo(Duration.ofHours(24)) // already exactly 09:00 -> rolls to tomorrow
        assertThat(nyDelay).isNotEqualTo(tokyoDelay)
    }

    @Test
    fun `fixed UTC offset zone has no DST transitions to worry about`() {
        val clock = fixedClock("2026-03-08T10:00:00Z", ZoneOffset.UTC)
        val target = LocalTime.of(12, 0)

        assertThat(durationUntilNext(target, clock)).isEqualTo(Duration.ofHours(2))
    }
}
