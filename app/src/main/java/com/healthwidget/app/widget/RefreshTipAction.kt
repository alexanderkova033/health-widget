package com.healthwidget.app.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.healthwidget.app.HealthWidgetApp
import java.time.LocalTime

/**
 * Bound to a tap on the widget's tip card: lets the user pull a new tip straight from the
 * home screen, without opening the app. Same selection/anti-repeat logic and shared "last
 * tip" persistence (FR5) as the scheduled refresh in [WidgetRefreshWorker], so this doesn't
 * create a second source of truth — it just runs that logic out of turn.
 */
class RefreshTipAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val container = (context.applicationContext as HealthWidgetApp).container
        container.advanceTip(LocalTime.now())
        container.refreshWidget()
    }
}
