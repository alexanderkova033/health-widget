package com.healthwidget.app

import android.app.Application
import com.healthwidget.app.notifications.NotificationHelper
import com.healthwidget.app.notifications.NudgeScheduler
import com.healthwidget.app.widget.WidgetScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class HealthWidgetApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }

    // Lives as long as the process; used only to fire-and-forget the startup scheduling
    // safety net below, never for long-running work.
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.ensureChannels(this)
        applicationScope.launch {
            val settings = container.settingsRepository.settings.first()
            NudgeScheduler(this@HealthWidgetApp).ensureScheduled(settings)
            WidgetScheduler(this@HealthWidgetApp).ensureScheduled(settings.widgetRefreshInterval.minutes)
        }
    }
}
