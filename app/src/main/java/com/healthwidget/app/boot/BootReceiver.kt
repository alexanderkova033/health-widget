package com.healthwidget.app.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.healthwidget.app.HealthWidgetApp
import com.healthwidget.app.notifications.NudgeScheduler
import com.healthwidget.app.widget.WidgetScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * FR7: WorkManager itself survives reboot, but this makes rescheduling explicit and also
 * forces one immediate widget refresh so the widget isn't stale until the next periodic tick.
 *
 * The device can plausibly come back up in a different time zone or with a corrected clock
 * (RTC drift, NTP sync, a SIM swap) after being off across the downtime, so nudge/sleep-alert
 * scheduling is fully recomputed and replaced here ([NudgeScheduler.rescheduleAll]) rather than
 * merely topped up if missing ([NudgeScheduler.ensureScheduled]) — the same policy used for an
 * explicit clock/time-zone-change broadcast (see [com.healthwidget.app.notifications.ClockChangeReceiver]).
 * The widget's periodic refresh has no wall-clock target to get wrong, so it stays on the
 * cheaper "only schedule if missing" policy.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val container = (appContext as HealthWidgetApp).container
                val settings = container.settingsRepository.settings.first()
                NudgeScheduler(appContext).rescheduleAll(settings)
                WidgetScheduler(appContext).apply {
                    ensureScheduled()
                    refreshNow()
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
