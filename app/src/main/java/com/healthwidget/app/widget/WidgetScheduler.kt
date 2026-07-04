package com.healthwidget.app.widget

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class WidgetScheduler(private val context: Context) {
    /** Safe to call on every app start / boot: won't reset an already-running periodic timer. */
    fun ensureScheduled() {
        val request =
            PeriodicWorkRequestBuilder<WidgetRefreshWorker>(REFRESH_INTERVAL_HOURS, TimeUnit.HOURS)
                .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(PERIODIC_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    /** Forces an immediate refresh, e.g. right after boot so the widget isn't stale until
     * the next periodic tick. */
    fun refreshNow() {
        val request = OneTimeWorkRequestBuilder<WidgetRefreshWorker>().build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(IMMEDIATE_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    companion object {
        private const val PERIODIC_WORK_NAME = "widget_periodic_refresh"
        private const val IMMEDIATE_WORK_NAME = "widget_immediate_refresh"

        // FR1: "at least every 2 hours" — 2h is the least-frequent interval that satisfies it.
        private const val REFRESH_INTERVAL_HOURS = 2L
    }
}
