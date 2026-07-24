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
     *
     * [manual] distinguishes an explicit user request for a new tip (a widget tap, the
     * Settings refresh button) from the passive scheduled rotation. The passive rotation's
     * fixed sleep-hours messages are deliberately exempt from anti-repeat (there's only one
     * possible message for each), but that exemption made a manual tap during those ~7 hours
     * (23:00-05:59) a silent no-op — the same fixed message came back every time, with no way
     * for the user to see it actually changed. A manual advance always draws from the general
     * pool instead, so tapping visibly does something regardless of time of day.
     *
     * [moreVarietyEnabled] is the Settings "more variety" toggle — see [pick] for how it's
     * applied. Defaults to `false` so every existing caller (and test) that doesn't know about
     * it yet keeps today's behavior exactly.
     */
    fun messageFor(
        time: LocalTime,
        recentTips: List<String>,
        manual: Boolean = false,
        moreVarietyEnabled: Boolean = false,
    ): Tip {
        val tonePool = catalog.philosophical + catalog.lighthearted
        return when (dayPartFor(time)) {
            DayPart.SLEEP_LATE ->
                if (manual) pick(catalog.general, tonePool, recentTips, moreVarietyEnabled) else catalog.sleepLate
            DayPart.SLEEP_EARLY_HOURS ->
                if (manual) pick(catalog.general, tonePool, recentTips, moreVarietyEnabled) else catalog.sleepEarlyHours
            DayPart.MORNING -> pick(catalog.general + catalog.morning, tonePool, recentTips, moreVarietyEnabled)
            DayPart.AFTERNOON -> pick(catalog.general + catalog.afternoon, tonePool, recentTips, moreVarietyEnabled)
            DayPart.EVENING -> pick(catalog.general + catalog.evening, tonePool, recentTips, moreVarietyEnabled)
        }
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
                catalog.philosophical + catalog.lighthearted +
                listOf(catalog.sleepLate, catalog.sleepEarlyHours)
        ).find { it.text == text }

    /**
     * [moreVarietyEnabled] is a *lean*, not a filter: toggling it never removes either group
     * entirely, it just flips which one is the overwhelming majority of the draw
     * ([TONE_DOMINANT_CHANCE_PERCENT] vs [TONE_MINORITY_CHANCE_PERCENT]) — off still lets a
     * philosophical/lighthearted tip through occasionally, on still leaves room for a practical
     * one, so the toggle reads as "mostly this" rather than "only this." The group-vs-group
     * coin flip only happens when [tonePool] actually has content — while it's empty (no
     * philosophical/lighthearted tips written yet, see [TipCatalog.philosophical]), this always
     * resolves to [practicalPool] with a single random draw, identical to before this toggle
     * existed.
     */
    private fun pick(
        practicalPool: List<Tip>,
        tonePool: List<Tip>,
        recentTips: List<String>,
        moreVarietyEnabled: Boolean,
    ): Tip {
        val pool = selectGroup(practicalPool, tonePool, moreVarietyEnabled)
        // If excluding every recent tip empties the pool (e.g. a small pool combined with a
        // long history), fall back to the full pool rather than crashing — this necessarily
        // repeats something, but only when there's truly no alternative left.
        val candidates = pool.filterNot { it.text in recentTips }.ifEmpty { pool }
        return candidates[random.nextInt(candidates.size)]
    }

    private fun selectGroup(
        practicalPool: List<Tip>,
        tonePool: List<Tip>,
        moreVarietyEnabled: Boolean,
    ): List<Tip> {
        if (tonePool.isEmpty()) return practicalPool
        if (practicalPool.isEmpty()) return tonePool
        val toneChancePercent = if (moreVarietyEnabled) TONE_DOMINANT_CHANCE_PERCENT else TONE_MINORITY_CHANCE_PERCENT
        return if (random.nextInt(100) < toneChancePercent) tonePool else practicalPool
    }

    private companion object {
        const val TONE_DOMINANT_CHANCE_PERCENT = 80
        const val TONE_MINORITY_CHANCE_PERCENT = 20
    }
}
