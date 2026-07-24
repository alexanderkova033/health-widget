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
import java.time.Clock
import java.time.LocalTime

/**
 * Fires once for a single daily nudge "slot" (see [NudgeScheduler]), then reschedules itself
 * ~24h ahead so the slot keeps recurring without needing external re-triggering.
 *
 * Mirrors [SleepAlertWorker]'s "if disabled, let the chain simply stop" pattern: both the
 * notification and the reschedule are gated on [com.healthwidget.core.settings.AppSettings.notificationsEnabled],
 * not just quiet hours. `NudgeScheduler.rescheduleAll` cancels this worker's pending unique
 * work the moment the master switch is turned off, but that's a separate write to WorkManager
 * racing an already-dispatched worker's own reschedule — without this check here too, a
 * worker that was already running when the switch flipped off could win that race and
 * resurrect the "cancelled" chain by re-enqueueing itself for tomorrow regardless.
 *
 * [clock] defaults to a fresh [Clock.systemDefaultZone] (not a value captured once at class
 * load) so both the quiet-hours check and the reschedule below always use the zone/offset in
 * effect right now; the extra constructor (via `@JvmOverloads`) exists so tests can substitute
 * a fixed [Clock] while WorkManager's default reflective instantiation — which requires a
 * plain `(Context, WorkerParameters)` constructor — keeps working unchanged.
 */
class NudgeWorker
    @JvmOverloads
    constructor(
        appContext: Context,
        params: WorkerParameters,
        private val clock: Clock = Clock.systemDefaultZone(),
    ) : CoroutineWorker(appContext, params) {
        override suspend fun doWork(): Result {
            val container = (applicationContext as HealthWidgetApp).container
            val slot = inputData.getInt(KEY_SLOT_INDEX, -1)
            if (slot < 0) return Result.failure()

            val settings = container.settingsRepository.settings.first()
            val now = LocalTime.now(clock)

            if (settings.notificationsEnabled) {
                if (!QuietHours.isWithin(now, settings.quietHoursStart, settings.quietHoursEnd)) {
                    val tip = container.advanceTip(now)
                    NotificationHelper.showNudge(applicationContext, tip.text)
                }
                rescheduleTomorrow(settings, slot)
            }

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
                    .setInitialDelay(durationUntilNext(times[slot], clock))
                    .setInputData(workDataOf(KEY_SLOT_INDEX to slot))
                    .build()
            WorkManager.getInstance(applicationContext)
                .enqueueUniqueWork(NudgeScheduler.nudgeWorkName(slot), ExistingWorkPolicy.REPLACE, request)
        }

        companion object {
            const val KEY_SLOT_INDEX = "slot_index"
        }
    }
