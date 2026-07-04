package com.healthwidget.core.scheduling

import java.time.LocalTime

/**
 * Quiet-hours overlap check (FR4). Pure time-of-day math so it stays JVM-testable — the
 * window commonly spans midnight (e.g. the 23:30-07:00 default), which is the main thing
 * worth covering with tests.
 */
object QuietHours {
    /**
     * [start] is inclusive, [end] is exclusive. If [start] equals [end] the window is
     * treated as empty (never quiet) rather than "quiet all day" — an ambiguous input the
     * settings UI is expected to prevent, but this keeps the function total either way.
     */
    fun isWithin(
        now: LocalTime,
        start: LocalTime,
        end: LocalTime,
    ): Boolean =
        when {
            start == end -> false
            start < end -> now >= start && now < end
            else -> now >= start || now < end // window wraps past midnight
        }
}
