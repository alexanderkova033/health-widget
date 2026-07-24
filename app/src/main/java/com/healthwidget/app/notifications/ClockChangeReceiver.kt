package com.healthwidget.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.healthwidget.app.HealthWidgetApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * The DST/time-zone-safe delay computed by [com.healthwidget.core.scheduling.durationUntilNext]
 * is only as good as the clock/zone it was computed against at enqueue time — a manual clock
 * change or a time-zone change after that point can leave an already-pending nudge/sleep-alert
 * work item targeting the wrong wall-clock moment. Both broadcasts here are exempt from
 * Android's implicit-broadcast manifest restrictions (like `BOOT_COMPLETED`) and are
 * system-protected, so a manifest-declared, `exported=true` receiver is safe and sufficient —
 * no persistent runtime-registered listener is needed.
 *
 * Rather than checking whether work already exists, this always fully recomputes and replaces
 * it ([NudgeScheduler.rescheduleAll], `ExistingWorkPolicy.REPLACE`) under the same unique work
 * names, so there is never more than one pending item per slot.
 */
class ClockChangeReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != Intent.ACTION_TIMEZONE_CHANGED && intent.action != Intent.ACTION_TIME_CHANGED) return

        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val container = (appContext as HealthWidgetApp).container
                val settings = container.settingsRepository.settings.first()
                NudgeScheduler(appContext).rescheduleAll(settings)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
