package com.healthwidget.core.tips

import kotlinx.coroutines.flow.first
import java.time.LocalTime

/**
 * Picks the next tip for [now] and persists it as the shared "last tip" — the one rule that
 * the nudge worker, the widget refresh worker, and the widget's first-render fallback must
 * all apply identically for the anti-repeat guarantee (FR5) to actually hold. Pulled out here
 * so those three call sites share one implementation instead of each re-deriving it.
 */
class AdvanceTipUseCase(
    private val tipEngine: TipEngine,
    private val tipHistoryRepository: TipHistoryRepository,
) {
    suspend operator fun invoke(now: LocalTime): String {
        val lastTip = tipHistoryRepository.lastTip.first()
        val tip = tipEngine.messageFor(now, lastTip)
        tipHistoryRepository.setLastTip(tip)
        return tip
    }
}
