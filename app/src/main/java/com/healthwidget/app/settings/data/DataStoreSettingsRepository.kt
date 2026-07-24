package com.healthwidget.app.settings.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.healthwidget.core.settings.AppSettings
import com.healthwidget.core.settings.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * [SettingsRepository] backed by Jetpack DataStore. Depends on [DataStore] rather than
 * [android.content.Context] so it can be unit-tested on the plain JVM with an
 * in-memory-backed DataStore, with no Robolectric/Android dependency.
 */
class DataStoreSettingsRepository(private val dataStore: DataStore<Preferences>) : SettingsRepository {
    override val settings: Flow<AppSettings> = dataStore.data.map { it.toAppSettings() }

    override suspend fun setMoreVarietyEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.MORE_VARIETY_ENABLED] = enabled }
    }

    private fun Preferences.toAppSettings(): AppSettings =
        AppSettings(
            moreVarietyEnabled = this[Keys.MORE_VARIETY_ENABLED] ?: AppSettings.DEFAULT.moreVarietyEnabled,
        )

    private object Keys {
        val MORE_VARIETY_ENABLED = booleanPreferencesKey("more_variety_enabled")
    }
}
