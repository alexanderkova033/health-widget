package com.healthwidget.app.notifications

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.healthwidget.app.data.AppSettings
import com.healthwidget.core.durationUntilNext
import java.time.LocalTime

/**
 * WorkManager has no "run at this clock time every day" primitive, so each nudge/sleep-alert
 * worker reschedules itself ~24h ahead after it runs (see [NudgeWorker], [SleepAlertWorker]).
 * This owns the unique work names and the initial scheduling so both entry points
 * ([rescheduleAll] on a settings change, [ensureScheduled] as an app-start safety net) stay
 * consistent.
 */
class NudgeScheduler(private val context: Context) {
    private val workManager: WorkManager
        get() = WorkManager.getInstance(context)

    /** Applies [settings] immediately, replacing any previously scheduled work. */
    fun rescheduleAll(settings: AppSettings) = apply(settings, ExistingWorkPolicy.REPLACE)

    /** Schedules work only where nothing is already pending; safe to call on every app start. */
    fun ensureScheduled(settings: AppSettings) = apply(settings, ExistingWorkPolicy.KEEP)

    private fun apply(
        settings: AppSettings,
        policy: ExistingWorkPolicy,
    ) {
        val nudgeTimes = NUDGE_TIMES_BY_FREQUENCY.getValue(settings.notificationFrequency)
        for (slot in 0 until MAX_NUDGE_SLOTS) {
            val workName = nudgeWorkName(slot)
            if (slot in nudgeTimes.indices) {
                enqueueNudge(slot, nudgeTimes[slot], workName, policy)
            } else {
                workManager.cancelUniqueWork(workName)
            }
        }

        if (settings.sleepAlertEnabled) {
            enqueueSleepAlert(policy)
        } else {
            workManager.cancelUniqueWork(SLEEP_ALERT_WORK_NAME)
        }
    }

    private fun enqueueNudge(
        slot: Int,
        time: LocalTime,
        workName: String,
        policy: ExistingWorkPolicy,
    ) {
        val request =
            OneTimeWorkRequestBuilder<NudgeWorker>()
                .setInitialDelay(durationUntilNext(time))
                .setInputData(workDataOf(NudgeWorker.KEY_SLOT_INDEX to slot))
                .build()
        workManager.enqueueUniqueWork(workName, policy, request)
    }

    private fun enqueueSleepAlert(policy: ExistingWorkPolicy) {
        val request =
            OneTimeWorkRequestBuilder<SleepAlertWorker>()
                .setInitialDelay(durationUntilNext(SLEEP_ALERT_TIME))
                .build()
        workManager.enqueueUniqueWork(SLEEP_ALERT_WORK_NAME, policy, request)
    }

    companion object {
        val SLEEP_ALERT_TIME: LocalTime = LocalTime.of(23, 0)
        const val SLEEP_ALERT_WORK_NAME = "sleep_alert"
        val MAX_NUDGE_SLOTS = AppSettings.MAX_NOTIFICATION_FREQUENCY

        // Product spec fixes frequency (0-3/day, FR2) but leaves exact times unspecified
        // ("user-visible times"). v1 picks simple fixed slots instead of a time picker, to
        // keep setup zero-friction; revisit if user feedback wants custom times.
        val NUDGE_TIMES_BY_FREQUENCY: Map<Int, List<LocalTime>> =
            mapOf(
                0 to emptyList(),
                1 to listOf(LocalTime.of(12, 0)),
                2 to listOf(LocalTime.of(10, 0), LocalTime.of(16, 0)),
                3 to listOf(LocalTime.of(10, 0), LocalTime.of(14, 0), LocalTime.of(18, 0)),
            )

        fun nudgeWorkName(slot: Int) = "nudge_slot_$slot"
    }
}
