package com.healthwidget.core.tips

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
     * [recentTips] are the most recently shown tips, across widget refreshes and
     * notifications combined, oldest first — excluded here so none of them repeats within
     * that span (see [TipHistoryRepository.MAX_RECENT_TIPS]).
     */
    fun messageFor(
        time: LocalTime,
        recentTips: List<String>,
    ): Tip =
        when (dayPartFor(time)) {
            // Fixed, single-message reminders are not drawn from a rotating pool — there is
            // only ever one possible message for each — so they are exempt from anti-repeat.
            DayPart.SLEEP_LATE -> catalog.sleepLate
            DayPart.SLEEP_EARLY_HOURS -> catalog.sleepEarlyHours
            DayPart.MORNING -> pick(catalog.general + catalog.morning, recentTips)
            DayPart.AFTERNOON -> pick(catalog.general + catalog.afternoon, recentTips)
            DayPart.EVENING -> pick(catalog.general + catalog.evening, recentTips)
        }

    /**
     * Resolves a tip's full [Tip] (including its citation) from just its displayed text — the
     * form [TipHistoryRepository] persists. Lets callers (e.g. the Settings screen) look up the
     * source of "whatever was last shown" without the history itself needing to store anything
     * beyond plain text.
     */
    fun findByText(text: String): Tip? =
        (
            catalog.general + catalog.morning + catalog.afternoon + catalog.evening +
                listOf(catalog.sleepLate, catalog.sleepEarlyHours)
        ).find { it.text == text }

    private fun pick(
        pool: List<Tip>,
        recentTips: List<String>,
    ): Tip {
        // If excluding every recent tip empties the pool (e.g. a small pool combined with a
        // long history), fall back to the full pool rather than crashing — this necessarily
        // repeats something, but only when there's truly no alternative left.
        val candidates = pool.filterNot { it.text in recentTips }.ifEmpty { pool }
        return candidates[random.nextInt(candidates.size)]
    }
}
