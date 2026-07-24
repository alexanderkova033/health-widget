package com.healthwidget.core.tips

import kotlinx.coroutines.flow.first
import java.time.LocalTime

/**
 * Picks the next tip for [now] and records it in the shared recent-tips history — the one
 * rule that the nudge worker, the widget refresh worker, and the widget's first-render
 * fallback must all apply identically for the anti-repeat guarantee (FR5, no repeats within
 * the last [TipHistoryRepository.MAX_RECENT_TIPS] tips) to actually hold. Pulled out here so
 * those three call sites share one implementation instead of each re-deriving it.
 */
class AdvanceTipUseCase(
    private val tipEngine: TipEngine,
    private val tipHistoryRepository: TipHistoryRepository,
) {
    suspend operator fun invoke(now: LocalTime): String {
        val recentTips = tipHistoryRepository.recentTips.first()
        val tip = tipEngine.messageFor(now, recentTips)
        tipHistoryRepository.recordTip(tip)
        return tip
    }
}
