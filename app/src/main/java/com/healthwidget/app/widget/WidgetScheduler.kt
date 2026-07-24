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
    fun ensureScheduled() = apply(DEFAULT_INTERVAL_MINUTES, ExistingPeriodicWorkPolicy.KEEP)

    private fun apply(
        intervalMinutes: Long,
        policy: ExistingPeriodicWorkPolicy,
    ) {
        val request =
            PeriodicWorkRequestBuilder<WidgetRefreshWorker>(intervalMinutes, TimeUnit.MINUTES)
                .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(PERIODIC_WORK_NAME, policy, request)
    }

    /** Forces an immediate refresh, e.g. right after boot so the widget isn't stale until
     * the next periodic tick. */
    fun refreshNow() {
        val request = OneTimeWorkRequestBuilder<WidgetRefreshWorker>().build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(IMMEDIATE_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    companion object {
        // FR1 requires the widget refresh at least every 2 hours; no longer user-configurable.
        private const val DEFAULT_INTERVAL_MINUTES = 120L
        private const val PERIODIC_WORK_NAME = "widget_periodic_refresh"
        private const val IMMEDIATE_WORK_NAME = "widget_immediate_refresh"
    }
}
