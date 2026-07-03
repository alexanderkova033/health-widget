package com.healthwidget.app

import android.content.Context
import com.healthwidget.app.data.SettingsRepository
import com.healthwidget.app.data.TipHistoryRepository
import com.healthwidget.app.data.healthWidgetDataStore
import com.healthwidget.core.TipEngine

/**
 * Small hand-written composition root. The fixed tech stack has no DI framework, and this
 * app is too small to justify pulling one in — a handful of `by lazy` singletons is enough.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(appContext.healthWidgetDataStore)
    }

    val tipHistoryRepository: TipHistoryRepository by lazy {
        TipHistoryRepository(appContext.healthWidgetDataStore)
    }

    val tipEngine: TipEngine by lazy { TipEngine() }
}
