package com.healthwidget.app.widget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.healthwidget.app.HealthWidgetApp
import java.time.LocalTime
import kotlinx.coroutines.flow.first

/**
 * Advances the widget's tip on a schedule (FR1: at least every 2 hours) by picking a new
 * tip, persisting it as the shared "last tip" (so notifications won't repeat it either, and
 * vice versa — FR5), then re-rendering every placed widget instance.
 *
 * Quiet hours intentionally do not gate this: they only silence nudge *notifications* (FR4);
 * a passive widget refresh isn't an interruption.
 */
class WidgetRefreshWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as HealthWidgetApp).container
        val lastTip = container.tipHistoryRepository.lastTip.first()
        val tip = container.tipEngine.messageFor(LocalTime.now(), lastTip)
        container.tipHistoryRepository.setLastTip(tip)

        TipWidget().updateAll(applicationContext)
        return Result.success()
    }
}
