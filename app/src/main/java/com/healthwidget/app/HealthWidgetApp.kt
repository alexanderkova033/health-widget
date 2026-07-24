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

    // Lives as long as the process — for fire-and-forget work that must survive whatever UI
    // triggered it (e.g. the startup scheduling safety net below, or a widget re-render
    // kicked off from the settings screen), never for long-running work. A screen-scoped
    // rememberCoroutineScope() would get cancelled if the user navigates away mid-flight.
    internal val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.ensureChannels(this)
        applicationScope.launch {
            val settings = container.settingsRepository.settings.first()
            NudgeScheduler(this@HealthWidgetApp).ensureScheduled(settings)
            WidgetScheduler(this@HealthWidgetApp).ensureScheduled()
        }
    }
}
