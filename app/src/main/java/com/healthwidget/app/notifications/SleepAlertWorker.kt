package com.healthwidget.app.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.healthwidget.app.HealthWidgetApp
import com.healthwidget.core.scheduling.durationUntilNext
import kotlinx.coroutines.flow.first
import java.time.Clock

/**
 * Fires at 23:00 local time (FR3), exempt from quiet hours by design. Only reschedules
 * itself for the next day while the sleep alert stays enabled; if the user disables it, the
 * chain simply stops, and [NudgeScheduler.rescheduleAll] re-enqueues it if re-enabled later.
 *
 * See [NudgeWorker] for why [clock] is a fresh-per-instance default with a `@JvmOverloads`
 * secondary constructor rather than a cached field or a required parameter.
 */
class SleepAlertWorker
    @JvmOverloads
    constructor(
        appContext: Context,
        params: WorkerParameters,
        private val clock: Clock = Clock.systemDefaultZone(),
    ) : CoroutineWorker(appContext, params) {
        override suspend fun doWork(): Result {
            val container = (applicationContext as HealthWidgetApp).container
            val settings = container.settingsRepository.settings.first()

            if (settings.notificationsEnabled && settings.sleepAlertEnabled) {
                val message = container.tipEngine.messageFor(NudgeScheduler.SLEEP_ALERT_TIME, recentTips = emptyList())
                NotificationHelper.showSleepAlert(applicationContext, message.text)

                val request =
                    OneTimeWorkRequestBuilder<SleepAlertWorker>()
                        .setInitialDelay(durationUntilNext(NudgeScheduler.SLEEP_ALERT_TIME, clock))
                        .build()
                WorkManager.getInstance(applicationContext)
                    .enqueueUniqueWork(NudgeScheduler.SLEEP_ALERT_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
            }

            return Result.success()
        }
    }
