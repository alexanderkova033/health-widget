package com.healthwidget.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.healthwidget.app.HealthWidgetApp
import com.healthwidget.app.settings.presentation.SettingsActivity
import kotlinx.coroutines.flow.first
import java.time.LocalTime

/**
 * FR1: shows the current tip, tapping opens the settings screen. [WidgetRefreshWorker] is
 * what normally advances the tip and calls [GlanceAppWidget.updateAll]; this only computes a
 * fallback tip itself on the very first render (e.g. right after install, before any worker
 * has run yet), reusing the same persisted "last tip" that notifications also read/write so
 * the anti-repeat guarantee (FR5) holds across both surfaces.
 */
class TipWidget : GlanceAppWidget() {
    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        val container = (context.applicationContext as HealthWidgetApp).container
        val existingTip = container.tipHistoryRepository.lastTip.first()
        val tip = existingTip ?: container.advanceTip(LocalTime.now())

        provideContent {
            GlanceTheme {
                TipWidgetContent(tip)
            }
        }
    }
}

@Composable
private fun TipWidgetContent(tip: String) {
    // Glance has its own LocalContext (androidx.glance), distinct from Compose UI's — this
    // version of glance-appwidget's actionStartActivity only takes an Intent, there's no
    // reified actionStartActivity<Activity>() convenience overload.
    val context = LocalContext.current
    Box(
        modifier =
            GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .clickable(actionStartActivity(Intent(context, SettingsActivity::class.java)))
                .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column {
            Text(
                text = "HealthWidget",
                style =
                    TextStyle(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = GlanceTheme.colors.primary,
                    ),
            )
            Text(
                text = tip,
                style =
                    TextStyle(
                        fontSize = 14.sp,
                        color = GlanceTheme.colors.onSurface,
                    ),
            )
        }
    }
}
