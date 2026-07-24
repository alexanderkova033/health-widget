package com.healthwidget.core.tips

import kotlinx.coroutines.flow.Flow

/**
 * Domain-layer contract for the recently-shown tips (oldest first), which is what makes the
 * anti-repeat rule (FR5) hold across the widget and notifications combined, within a span of
 * [MAX_RECENT_TIPS] tips rather than just the single previous one. The DataStore-backed
 * implementation lives in `:app` (`DataStoreTipHistoryRepository`).
 */
interface TipHistoryRepository {
    val recentTips: Flow<List<String>>

    /** Appends [tip], trimming to the [MAX_RECENT_TIPS] most recent entries. */
    suspend fun recordTip(tip: String)

    companion object {
        const val MAX_RECENT_TIPS = 30
    }
}
