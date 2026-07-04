package com.healthwidget.core.tips

import kotlinx.coroutines.flow.Flow

/**
 * Domain-layer contract for the single most-recently-shown tip, which is what makes the
 * anti-repeat rule (FR5) hold across the widget and notifications combined. The
 * DataStore-backed implementation lives in `:app` (`DataStoreTipHistoryRepository`).
 */
interface TipHistoryRepository {
    val lastTip: Flow<String?>

    suspend fun setLastTip(tip: String)
}
