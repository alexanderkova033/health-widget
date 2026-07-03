package com.healthwidget.core

import java.time.LocalTime
import kotlin.random.Random

/**
 * Pure Kotlin, JVM-testable tip selection logic — no Android imports allowed in this module.
 *
 * [random] is injected (defaulting to [Random.Default]) purely so tests can supply a
 * deterministic source and assert anti-repeat behavior without relying on statistics.
 */
class TipEngine(
    private val catalog: TipCatalog = TipCatalog.loadDefault(),
    private val random: Random = Random.Default,
) {
    fun dayPartFor(time: LocalTime): DayPart =
        when (time.hour) {
            23 -> DayPart.SLEEP_LATE
            in 0..5 -> DayPart.SLEEP_EARLY_HOURS
            in 6..11 -> DayPart.MORNING
            in 12..17 -> DayPart.AFTERNOON
            else -> DayPart.EVENING // 18..22
        }

    /**
     * [lastTip] is the most recently shown tip, across widget refreshes and notifications
     * combined, so it can be excluded here to satisfy the no-back-to-back-repeats requirement.
     */
    fun messageFor(
        time: LocalTime,
        lastTip: String?,
    ): String =
        when (dayPartFor(time)) {
            // Fixed, single-message reminders are not drawn from a rotating pool — there is
            // only ever one possible message for each — so they are exempt from anti-repeat.
            DayPart.SLEEP_LATE -> catalog.sleepLate
            DayPart.SLEEP_EARLY_HOURS -> catalog.sleepEarlyHours
            DayPart.MORNING -> pick(catalog.general + catalog.morning, lastTip)
            DayPart.AFTERNOON -> pick(catalog.general + catalog.afternoon, lastTip)
            DayPart.EVENING -> pick(catalog.general + catalog.evening, lastTip)
        }

    private fun pick(
        pool: List<String>,
        lastTip: String?,
    ): String {
        // If excluding lastTip empties the pool (e.g. a pool of size 1), fall back to the
        // full pool rather than crashing — a single-tip pool necessarily repeats.
        val candidates = pool.filter { it != lastTip }.ifEmpty { pool }
        return candidates[random.nextInt(candidates.size)]
    }
}
