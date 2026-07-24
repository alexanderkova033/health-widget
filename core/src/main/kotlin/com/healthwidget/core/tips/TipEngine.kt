package com.healthwidget.core.tips

import com.healthwidget.core.settings.VarietyLevel
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
     * [varietyLevel] is the Settings variety level — see [pick] for how it's applied. Defaults
     * to [VarietyLevel.PRACTICAL] so every existing caller (and test) that doesn't know about
     * it yet keeps today's behavior exactly.
     */
    fun messageFor(
        time: LocalTime,
        recentTips: List<String>,
        manual: Boolean = false,
        varietyLevel: VarietyLevel = VarietyLevel.PRACTICAL,
    ): Tip {
        val tonePool = catalog.philosophical + catalog.lighthearted
        return when (dayPartFor(time)) {
            DayPart.SLEEP_LATE ->
                if (manual) pick(catalog.general, tonePool, recentTips, varietyLevel) else catalog.sleepLate
            DayPart.SLEEP_EARLY_HOURS ->
                if (manual) pick(catalog.general, tonePool, recentTips, varietyLevel) else catalog.sleepEarlyHours
            DayPart.MORNING -> pick(catalog.general + catalog.morning, tonePool, recentTips, varietyLevel)
            DayPart.AFTERNOON -> pick(catalog.general + catalog.afternoon, tonePool, recentTips, varietyLevel)
            DayPart.EVENING -> pick(catalog.general + catalog.evening, tonePool, recentTips, varietyLevel)
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
     * [varietyLevel] is a *lean*, not a filter: it never removes either group entirely, it just
     * shifts which one is favored in the draw ([VarietyLevel.toneChancePercent]) —
     * [VarietyLevel.PRACTICAL] still lets a philosophical/lighthearted tip through
     * occasionally, [VarietyLevel.PLAYFUL] still leaves room for a practical one, so every
     * level reads as "mostly this" rather than "only this."
     *
     * Anti-repeat is applied to *both* pools before the weighted choice, not after choosing one
     * — doing it the other way round (weight first, filter second) let the coin flip land on a
     * group whose only unseen tips had all just been shown, and repeat one of those while the
     * other group still had fresh options sitting right there unused. Only once *neither* pool
     * has anything unseen left does this fall back to the weighted full pools, which
     * necessarily repeats something.
     */
    private fun pick(
        practicalPool: List<Tip>,
        tonePool: List<Tip>,
        recentTips: List<String>,
        varietyLevel: VarietyLevel,
    ): Tip {
        val freshPractical = practicalPool.filterNot { it.text in recentTips }
        val freshTone = tonePool.filterNot { it.text in recentTips }
        val candidates =
            when {
                freshPractical.isEmpty() && freshTone.isEmpty() ->
                    selectGroup(practicalPool, tonePool, varietyLevel)
                freshTone.isEmpty() -> freshPractical
                freshPractical.isEmpty() -> freshTone
                else -> selectGroup(freshPractical, freshTone, varietyLevel)
            }
        return candidates[random.nextInt(candidates.size)]
    }

    /**
     * The group-vs-group coin flip only happens when both pools actually have content to choose
     * between — while [tonePool] is empty (no philosophical/lighthearted tips written yet, see
     * [TipCatalog.philosophical]), this always resolves to [practicalPool] with a single random
     * draw, identical to before the variety setting existed.
     */
    private fun selectGroup(
        practicalPool: List<Tip>,
        tonePool: List<Tip>,
        varietyLevel: VarietyLevel,
    ): List<Tip> {
        if (tonePool.isEmpty()) return practicalPool
        if (practicalPool.isEmpty()) return tonePool
        return if (random.nextInt(100) < varietyLevel.toneChancePercent) tonePool else practicalPool
    }

    /** The tone pool's odds of winning the [selectGroup] coin flip. [VarietyLevel.BALANCED]
     * sits at an even split; [VarietyLevel.PRACTICAL] and [VarietyLevel.PLAYFUL] keep the same
     * dominant/minority split the setting shipped with before it had a middle option. */
    private val VarietyLevel.toneChancePercent: Int
        get() =
            when (this) {
                VarietyLevel.PRACTICAL -> TONE_MINORITY_CHANCE_PERCENT
                VarietyLevel.BALANCED -> TONE_BALANCED_CHANCE_PERCENT
                VarietyLevel.PLAYFUL -> TONE_DOMINANT_CHANCE_PERCENT
            }

    private companion object {
        const val TONE_DOMINANT_CHANCE_PERCENT = 80
        const val TONE_BALANCED_CHANCE_PERCENT = 50
        const val TONE_MINORITY_CHANCE_PERCENT = 20
    }
}
