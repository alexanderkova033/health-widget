package com.healthwidget.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Persists the single most-recently-shown tip so [com.healthwidget.core.TipEngine]'s
 * anti-repeat rule (FR5) holds across widget refreshes and notifications combined, and
 * survives process death.
 */
class TipHistoryRepository(private val dataStore: DataStore<Preferences>) {
    val lastTip: Flow<String?> = dataStore.data.map { it[Keys.LAST_TIP] }

    suspend fun setLastTip(tip: String) {
        dataStore.edit { it[Keys.LAST_TIP] = tip }
    }

    private object Keys {
        val LAST_TIP = stringPreferencesKey("last_tip")
    }
}
