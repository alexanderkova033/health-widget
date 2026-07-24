package com.healthwidget.app.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.healthwidget.app.HealthWidgetApp
import com.healthwidget.core.scheduling.QuietHours
import com.healthwidget.core.scheduling.durationUntilNext
import com.healthwidget.core.settings.AppSettings
import kotlinx.coroutines.flow.first
import java.time.LocalTime

/**
 * Fires once for a single daily nudge "slot" (see [NudgeScheduler]), then reschedules itself
 * ~24h ahead so the slot keeps recurring without needing external re-triggering.
 */
class NudgeWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as HealthWidgetApp).container
        val slot = inputData.getInt(KEY_SLOT_INDEX, -1)
        if (slot < 0) return Result.failure()

        val settings = container.settingsRepository.settings.first()
        val now = LocalTime.now()

        if (!QuietHours.isWithin(now, settings.quietHoursStart, settings.quietHoursEnd)) {
            val tip = container.advanceTip(now)
            NotificationHelper.showNudge(applicationContext, tip.text)
        }

        rescheduleTomorrow(settings, slot)
        return Result.success()
    }

    private fun rescheduleTomorrow(
        settings: AppSettings,
        slot: Int,
    ) {
        val times = NudgeScheduler.NUDGE_TIMES_BY_FREQUENCY.getValue(settings.notificationFrequency)
        // Frequency may have dropped since this work was scheduled (e.g. 3 -> 1); if this
        // slot no longer exists, don't reschedule it. NudgeScheduler.rescheduleAll already
        // handles cancelling/re-adding slots on an explicit settings change.
        if (slot !in times.indices) return

        val request =
            OneTimeWorkRequestBuilder<NudgeWorker>()
                .setInitialDelay(durationUntilNext(times[slot]))
                .setInputData(workDataOf(KEY_SLOT_INDEX to slot))
                .build()
        WorkManager.getInstance(applicationContext)
            .enqueueUniqueWork(NudgeScheduler.nudgeWorkName(slot), ExistingWorkPolicy.REPLACE, request)
    }

    companion object {
        const val KEY_SLOT_INDEX = "slot_index"
    }
}
