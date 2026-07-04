package com.healthwidget.app

import android.content.Context
import com.healthwidget.app.common.healthWidgetDataStore
import com.healthwidget.app.settings.data.DataStoreSettingsRepository
import com.healthwidget.app.tips.data.DataStoreTipHistoryRepository
import com.healthwidget.core.settings.SettingsRepository
import com.healthwidget.core.tips.AdvanceTipUseCase
import com.healthwidget.core.tips.TipEngine
import com.healthwidget.core.tips.TipHistoryRepository

/**
 * Small hand-written composition root. The fixed tech stack has no DI framework, and this
 * app is too small to justify pulling one in — a handful of `by lazy` singletons is enough.
 *
 * Exposes the domain-layer interfaces ([SettingsRepository], [TipHistoryRepository]), not
 * the DataStore-backed implementation classes, so callers depend on the abstraction.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val settingsRepository: SettingsRepository by lazy {
        DataStoreSettingsRepository(appContext.healthWidgetDataStore)
    }

    val tipHistoryRepository: TipHistoryRepository by lazy {
        DataStoreTipHistoryRepository(appContext.healthWidgetDataStore)
    }

    val tipEngine: TipEngine by lazy { TipEngine() }

    val advanceTip: AdvanceTipUseCase by lazy { AdvanceTipUseCase(tipEngine, tipHistoryRepository) }
}
