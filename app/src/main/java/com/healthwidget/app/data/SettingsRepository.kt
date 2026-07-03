package com.healthwidget.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Depends on [DataStore] rather than [android.content.Context] so it can be unit-tested on
 * the plain JVM with an in-memory-backed DataStore, with no Robolectric/Android dependency.
 */
class SettingsRepository(private val dataStore: DataStore<Preferences>) {
    val settings: Flow<AppSettings> = dataStore.data.map { it.toAppSettings() }

    suspend fun setNotificationFrequency(frequency: Int) {
        require(frequency in AppSettings.MIN_NOTIFICATION_FREQUENCY..AppSettings.MAX_NOTIFICATION_FREQUENCY) {
            "notificationFrequency must be in " +
                "${AppSettings.MIN_NOTIFICATION_FREQUENCY}..${AppSettings.MAX_NOTIFICATION_FREQUENCY}, was $frequency"
        }
        dataStore.edit { it[Keys.NOTIFICATION_FREQUENCY] = frequency }
    }

    suspend fun setSleepAlertEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.SLEEP_ALERT_ENABLED] = enabled }
    }

    suspend fun setQuietHours(
        start: LocalTime,
        end: LocalTime,
    ) {
        dataStore.edit {
            it[Keys.QUIET_HOURS_START] = start.toString()
            it[Keys.QUIET_HOURS_END] = end.toString()
        }
    }

    private fun Preferences.toAppSettings(): AppSettings =
        AppSettings(
            notificationFrequency = this[Keys.NOTIFICATION_FREQUENCY] ?: AppSettings.DEFAULT.notificationFrequency,
            sleepAlertEnabled = this[Keys.SLEEP_ALERT_ENABLED] ?: AppSettings.DEFAULT.sleepAlertEnabled,
            quietHoursStart =
                this[Keys.QUIET_HOURS_START]?.let(LocalTime::parse) ?: AppSettings.DEFAULT.quietHoursStart,
            quietHoursEnd =
                this[Keys.QUIET_HOURS_END]?.let(LocalTime::parse) ?: AppSettings.DEFAULT.quietHoursEnd,
        )

    private object Keys {
        val NOTIFICATION_FREQUENCY = intPreferencesKey("notification_frequency")
        val SLEEP_ALERT_ENABLED = booleanPreferencesKey("sleep_alert_enabled")
        val QUIET_HOURS_START = stringPreferencesKey("quiet_hours_start")
        val QUIET_HOURS_END = stringPreferencesKey("quiet_hours_end")
    }
}
