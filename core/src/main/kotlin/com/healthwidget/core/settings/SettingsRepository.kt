package com.healthwidget.core.settings

import kotlinx.coroutines.flow.Flow
import java.time.LocalTime

/**
 * Domain-layer contract for reading/writing [AppSettings]. The DataStore-backed
 * implementation lives in `:app` (`DataStoreSettingsRepository`) — nothing in this module
 * knows or cares how settings are actually persisted.
 */
interface SettingsRepository {
    val settings: Flow<AppSettings>

    suspend fun setNotificationFrequency(frequency: Int)

    suspend fun setSleepAlertEnabled(enabled: Boolean)

    suspend fun setQuietHours(
        start: LocalTime,
        end: LocalTime,
    )

    suspend fun setWidgetStyle(style: WidgetStyle)

    suspend fun setWidgetRefreshInterval(interval: WidgetRefreshInterval)
}
