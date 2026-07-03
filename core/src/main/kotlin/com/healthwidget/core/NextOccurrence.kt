package com.healthwidget.core

import java.time.Duration
import java.time.LocalTime

/**
 * WorkManager has no "run at this exact clock time every day" primitive, so nudge/sleep-alert
 * workers reschedule themselves ~24h ahead after each run; this computes the delay to the
 * next occurrence of [target], relative to [now]. Kept pure/JVM-testable per the no-Android
 * business-logic rule, even though its only caller lives in the Android app module.
 */
fun durationUntilNext(
    target: LocalTime,
    now: LocalTime = LocalTime.now(),
): Duration {
    val nowSeconds = now.toSecondOfDay()
    val targetSeconds = target.toSecondOfDay()
    val secondsUntil =
        if (targetSeconds > nowSeconds) {
            targetSeconds - nowSeconds
        } else {
            (SECONDS_PER_DAY - nowSeconds) + targetSeconds
        }
    return Duration.ofSeconds(secondsUntil.toLong())
}

private const val SECONDS_PER_DAY = 24 * 60 * 60
