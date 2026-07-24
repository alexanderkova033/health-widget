package com.healthwidget.app

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.healthwidget.app.common.healthWidgetDataStore
import com.healthwidget.app.tips.data.DataStoreTipHistoryRepository
import com.healthwidget.app.widget.TipWidget
import com.healthwidget.app.widget.data.DataStoreWidgetRefreshRepository
import com.healthwidget.core.scheduling.WidgetRefreshRepository
import com.healthwidget.core.tips.AdvanceTipUseCase
import com.healthwidget.core.tips.TipEngine
import com.healthwidget.core.tips.TipHistoryRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Small hand-written composition root. The fixed tech stack has no DI framework, and this
 * app is too small to justify pulling one in — a handful of `by lazy` singletons is enough.
 *
 * Exposes the domain-layer interface ([TipHistoryRepository]), not the DataStore-backed
 * implementation class, so callers depend on the abstraction.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    // Serializes every push of the widget's UI to the home screen. Three independent triggers
    // can call this (the periodic tick worker, the manual "get a different tip" button, and
    // tapping the widget itself) with no ordering guarantee between them — without a lock, two
    // overlapping GlanceAppWidget.updateAll() calls could finish out of order and leave a stale
    // render on screen (the real, reported cause of the widget background/tip only *sometimes*
    // updating). Each call still re-reads the current tip from DataStore at execution time
    // (nothing captures a stale snapshot), so serializing just the push itself is enough:
    // whichever call runs last always renders whatever is actually persisted, regardless of
    // which trigger queued first.
    private val widgetRefreshMutex = Mutex()

    val tipHistoryRepository: TipHistoryRepository by lazy {
        DataStoreTipHistoryRepository(appContext.healthWidgetDataStore)
    }

    val widgetRefreshRepository: WidgetRefreshRepository by lazy {
        DataStoreWidgetRefreshRepository(appContext.healthWidgetDataStore)
    }

    val tipEngine: TipEngine by lazy { TipEngine() }

    val advanceTip: AdvanceTipUseCase by lazy { AdvanceTipUseCase(tipEngine, tipHistoryRepository) }

    suspend fun refreshWidget() {
        widgetRefreshMutex.withLock {
            TipWidget().updateAll(appContext)
        }
    }
}
