package com.healthwidget.app.common

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

private const val PREFS_NAME = "healthwidget_prefs"

// Single DataStore file backs both settings and tip history: the whole app is a single
// process (widget + workers + UI all run in-process), so there is no multi-process
// contention that would justify splitting into multiple files.
val Context.healthWidgetDataStore: DataStore<Preferences> by preferencesDataStore(name = PREFS_NAME)
