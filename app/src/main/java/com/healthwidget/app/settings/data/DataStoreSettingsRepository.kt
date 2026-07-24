package com.healthwidget.app.settings.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.healthwidget.core.settings.AppSettings
import com.healthwidget.core.settings.SettingsRepository
import com.healthwidget.core.settings.WidgetStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * [SettingsRepository] backed by Jetpack DataStore. Depends on [DataStore] rather than
 * [android.content.Context] so it can be unit-tested on the plain JVM with an
 * in-memory-backed DataStore, with no Robolectric/Android dependency.
 */
class DataStoreSettingsRepository(private val dataStore: DataStore<Preferences>) : SettingsRepository {
    override val settings: Flow<AppSettings> = dataStore.data.map { it.toAppSettings() }

    override suspend fun setWidgetStyle(style: WidgetStyle) {
        dataStore.edit { it[Keys.WIDGET_STYLE] = style.name }
    }

    private fun Preferences.toAppSettings(): AppSettings =
        AppSettings(
            widgetStyle = this[Keys.WIDGET_STYLE]?.toEnumOrNull<WidgetStyle>() ?: AppSettings.DEFAULT.widgetStyle,
        )

    private inline fun <reified T : Enum<T>> String.toEnumOrNull(): T? = enumValues<T>().firstOrNull { it.name == this }

    private object Keys {
        val WIDGET_STYLE = stringPreferencesKey("widget_style")
    }
}
