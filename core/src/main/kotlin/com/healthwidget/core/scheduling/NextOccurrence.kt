package com.healthwidget.core.scheduling

import java.time.Clock
import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime

/**
 * WorkManager has no "run at this exact clock time every day" primitive, so nudge/sleep-alert
 * workers reschedule themselves ~24h ahead after each run; this computes the delay to the
 * next occurrence of [target] local clock time, relative to [clock].
 *
 * Built on [ZonedDateTime] (rather than a naive `LocalTime` + fixed 24h/[Duration] delta) so a
 * pending delay stays correct across a daylight-saving transition, a time-zone change, or a
 * manual clock change that happens between "now" and the target time: the returned [Duration]
 * always lands on the wall-clock target, not on "24 hours from now" (which the old
 * `LocalTime`-only version silently assumed, and which is exactly wrong across a DST shift).
 *
 * [clock] carries both the current instant and the zone to interpret [target] in, so it must be
 * created fresh at the point of use (e.g. `Clock.systemDefaultZone()`) rather than cached in a
 * long-lived field — the zone it captures is frozen at creation time, same as `ZoneId.systemDefault()`.
 *
 * Ambiguous local times around a DST transition are resolved using [ZonedDateTime]'s documented
 * default behavior, which this module relies on rather than reimplementing: a target that falls
 * in a spring-forward *gap* (skipped local time) is pushed forward by the length of the gap; a
 * target that falls in an autumn-fallback *overlap* (a local time that occurs twice) resolves to
 * its earlier occurrence.
 *
 * Kept pure/JVM-testable per the no-Android business-logic rule, even though its only caller
 * lives in the Android app module.
 */
fun durationUntilNext(
    target: LocalTime,
    clock: Clock,
): Duration {
    val now = ZonedDateTime.now(clock)
    val todayAtTarget = ZonedDateTime.of(now.toLocalDate(), target, clock.zone)
    val next = if (todayAtTarget.toInstant() > now.toInstant()) todayAtTarget else todayAtTarget.plusDays(1)
    return Duration.between(now.toInstant(), next.toInstant())
}
