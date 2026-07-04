package com.healthwidget.app.tips.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.healthwidget.core.tips.TipHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * [TipHistoryRepository] backed by Jetpack DataStore, so the last-shown tip survives process
 * death (FR7) and is shared between the widget and notifications (FR5).
 */
class DataStoreTipHistoryRepository(private val dataStore: DataStore<Preferences>) : TipHistoryRepository {
    override val lastTip: Flow<String?> = dataStore.data.map { it[Keys.LAST_TIP] }

    override suspend fun setLastTip(tip: String) {
        dataStore.edit { it[Keys.LAST_TIP] = tip }
    }

    private object Keys {
        val LAST_TIP = stringPreferencesKey("last_tip")
    }
}
