package com.healthwidget.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.healthwidget.app.HealthWidgetApp
import com.healthwidget.app.R
import com.healthwidget.app.settings.presentation.SettingsActivity
import com.healthwidget.core.settings.WidgetStyle
import kotlinx.coroutines.flow.first
import java.time.LocalTime

/**
 * FR1: shows the current tip. Tapping the card itself gets a new tip on the spot (via
 * [RefreshTipAction], without opening the app); the small gear icon in the corner is the
 * only way into the settings screen — AppWidgets can't intercept long-press, the launcher
 * reserves that gesture for its own move/resize/remove UI, so a dedicated tap target is the
 * only reliable option. [WidgetRefreshWorker] is what normally advances the tip on a timer
 * and calls [GlanceAppWidget.updateAll]; this only computes a fallback tip itself on the very
 * first render (e.g. right after install, before any worker has run yet), reusing the same
 * persisted "last tip" that notifications also read/write so the anti-repeat guarantee (FR5)
 * holds across both surfaces.
 */
class TipWidget : GlanceAppWidget() {
    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        val container = (context.applicationContext as HealthWidgetApp).container
        val existingTip = container.tipHistoryRepository.recentTips.first().lastOrNull()
        val tip = existingTip ?: container.advanceTip(LocalTime.now()).text
        val style = container.settingsRepository.settings.first().widgetStyle

        provideContent {
            GlanceTheme {
                TipWidgetContent(tip, style)
            }
        }
    }
}

internal fun WidgetStyle.backgroundDrawableRes(): Int =
    when (this) {
        WidgetStyle.FOREST -> R.drawable.widget_quote_background
        WidgetStyle.OCEAN -> R.drawable.widget_background_ocean
        WidgetStyle.SUNSET -> R.drawable.widget_background_sunset
        WidgetStyle.MIDNIGHT -> R.drawable.widget_background_midnight
    }

@Composable
private fun TipWidgetContent(
    tip: String,
    style: WidgetStyle,
) {
    // Glance has its own LocalContext (androidx.glance), distinct from Compose UI's — this
    // version of glance-appwidget's actionStartActivity only takes an Intent, there's no
    // reified actionStartActivity<Activity>() convenience overload.
    val context = LocalContext.current

    // Styled like a quote-card widget (à la the "Motivation" app): a fixed brand gradient
    // with bold centered text, rather than following GlanceTheme's day/night surface colors
    // — the gradient itself provides the contrast, on both light and dark home screens.
    Box(
        modifier =
            GlanceModifier
                .fillMaxSize()
                .cornerRadius(20.dp)
                .background(ImageProvider(style.backgroundDrawableRes()))
                // Whole-card tap: refreshes the tip in place. The settings icon below has its
                // own clickable modifier, which takes the tap over this one within its bounds.
                .clickable(actionRunCallback<RefreshTipAction>())
                .padding(horizontal = 20.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                Spacer(GlanceModifier.defaultWeight())
                Image(
                    provider = ImageProvider(R.drawable.ic_widget_settings),
                    contentDescription = context.getString(R.string.widget_settings_action),
                    modifier =
                        GlanceModifier
                            .clickable(actionStartActivity(Intent(context, SettingsActivity::class.java)))
                            .padding(6.dp)
                            .size(20.dp),
                )
            }
            Box(
                modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = GlanceModifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "❝",
                        style =
                            TextStyle(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorProvider(Color.White.copy(alpha = 0.55f)),
                            ),
                    )
                    Spacer(GlanceModifier.height(4.dp))
                    Text(
                        text = tip,
                        maxLines = 5,
                        style =
                            TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = ColorProvider(Color.White),
                            ),
                    )
                    Spacer(GlanceModifier.height(10.dp))
                    Text(
                        // Derived from the centralized app_name resource (rather than a second
                        // hardcoded string) so changing the final product name only ever means
                        // editing strings.xml in one place.
                        text = context.getString(R.string.app_name).uppercase(),
                        style =
                            TextStyle(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                color = ColorProvider(Color.White.copy(alpha = 0.6f)),
                            ),
                    )
                }
            }
        }
    }
}
