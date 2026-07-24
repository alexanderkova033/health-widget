package com.healthwidget.app.widget

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.text.StaticLayout
import android.text.TextPaint
import android.util.DisplayMetrics
import android.util.TypedValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontFamily
import androidx.glance.text.FontStyle
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
    // Exact (rather than the default Single, which always reports the min size declared in
    // tip_widget_info.xml) so LocalSize.current reflects whatever size the user actually
    // resized this instance to — needed to shrink the tip's font to fit narrow placements
    // instead of clipping it, see fittingTipFontSizeSp below.
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        val container = (context.applicationContext as HealthWidgetApp).container
        val existingTip = container.tipHistoryRepository.recentTips.first().lastOrNull()
        val tip =
            existingTip ?: run {
                val moreVarietyEnabled = container.settingsRepository.settings.first().moreVarietyEnabled
                container.advanceTip(LocalTime.now(), moreVarietyEnabled = moreVarietyEnabled).text
            }
        val style = WidgetStyle.forTip(tip)

        provideContent {
            GlanceTheme {
                TipWidgetContent(tip, style)
            }
        }
    }
}

private fun WidgetStyle.backgroundDrawableRes(): Int =
    when (this) {
        WidgetStyle.FOREST -> R.drawable.widget_quote_background
        WidgetStyle.OCEAN -> R.drawable.widget_background_ocean
        WidgetStyle.SUNSET -> R.drawable.widget_background_sunset
        WidgetStyle.MIDNIGHT -> R.drawable.widget_background_midnight
    }

private const val TIP_FONT_MAX_SP = 20f

// A fixed floor of a few discrete steps could still be too big to fit the longest tips on the
// narrowest widget placements (that's exactly what produced the "…" truncation this replaces:
// the previous smallest step, 13sp, still didn't always fit) — 8sp is small enough that even
// the catalog's longest tip (~98 characters) wraps within maxLines at the widget's declared
// minimum width, so the binary search below always has a floor that actually fits.
private const val TIP_FONT_MIN_SP = 8f

/**
 * TextView's real auto-size setters (`setAutoSizeTextTypeWithDefaults` etc.) aren't annotated
 * `@RemotableViewMethod`, so RemoteViews/Glance widgets can't use live auto-sizing text — this
 * is the manual substitute: binary-search (rather than stepping through a few fixed sizes, which
 * either wastes space or still overflows depending on where the tip's actual length falls
 * between steps) the largest size between [TIP_FONT_MIN_SP] and [TIP_FONT_MAX_SP] that still
 * wraps the tip within [maxLines] at [availableWidthPx], measured with [StaticLayout] — the same
 * line-breaking engine a real TextView uses — so the size scales continuously with how long the
 * tip actually is. The paint's typeface is pinned to [Typeface.SERIF] to match the tip [Text]'s
 * own `FontFamily.Serif` below — serif glyphs measure a little wider than the default sans face,
 * so measuring with the wrong typeface here would let a line wrap that the actual rendered font
 * doesn't.
 */
private fun fittingTipFontSizeSp(
    tip: String,
    availableWidthPx: Int,
    maxLines: Int,
    metrics: DisplayMetrics,
): Float {
    if (availableWidthPx <= 0) return TIP_FONT_MIN_SP
    val paint =
        TextPaint().apply {
            isFakeBoldText = true
            typeface = Typeface.SERIF
        }
    fun fitsAt(sp: Float): Boolean {
        paint.textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, metrics)
        val lineCount =
            StaticLayout.Builder
                .obtain(tip, 0, tip.length, paint, availableWidthPx)
                .build()
                .lineCount
        return lineCount <= maxLines
    }
    if (fitsAt(TIP_FONT_MAX_SP)) return TIP_FONT_MAX_SP
    if (!fitsAt(TIP_FONT_MIN_SP)) return TIP_FONT_MIN_SP
    var lo = TIP_FONT_MIN_SP
    var hi = TIP_FONT_MAX_SP
    repeat(6) {
        val mid = (lo + hi) / 2f
        if (fitsAt(mid)) lo = mid else hi = mid
    }
    return lo
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

    // Root padding (20dp * 2) + the tip chip's own padding (14dp * 2) below, subtracted from
    // the widget's real current width (see TipWidget.sizeMode) to get the width the tip text
    // actually has to wrap within.
    val size = LocalSize.current
    val tipFontSizeSp =
        remember(tip, size) {
            val metrics = context.resources.displayMetrics
            val availableWidthPx = ((size.width.value - 68f) * metrics.density).toInt()
            fittingTipFontSizeSp(tip, availableWidthPx, maxLines = 6, metrics = metrics)
        }

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
        // The settings button used to sit in its own header Row above this Column, which
        // reserved a full-width strip purely for a 36dp circle and squeezed the quote+tip
        // block into whatever height was left. It's now a corner overlay (below) instead, so
        // this Column — and the quote+tip inside it — gets the card's entire height.
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
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
                                fontStyle = FontStyle.Italic,
                                fontFamily = FontFamily.Serif,
                                color = ColorProvider(Color.White.copy(alpha = 0.6f)),
                            ),
                    )
                    Spacer(GlanceModifier.height(4.dp))
                    // A translucent "chip" behind the tip, rather than bare text over the
                    // gradient/art: guarantees contrast no matter where on the gradient (or
                    // over which piece of background art) the text lands, and gives the tip
                    // its own visible frame instead of floating loose over the artwork.
                    Box(
                        modifier =
                            GlanceModifier
                                .background(ColorProvider(Color.Black.copy(alpha = 0.22f)))
                                .cornerRadius(14.dp)
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = tip,
                            maxLines = 6,
                            style =
                                TextStyle(
                                    fontSize = tipFontSizeSp.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Serif,
                                    textAlign = TextAlign.Center,
                                    color = ColorProvider(Color.White),
                                ),
                        )
                    }
                }
            }
            Spacer(GlanceModifier.height(8.dp))
            Text(
                // Derived from the centralized app_name resource (rather than a second
                // hardcoded string) so changing the final product name only ever means editing
                // strings.xml in one place.
                text = context.getString(R.string.app_name).uppercase(),
                style =
                    TextStyle(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        color = ColorProvider(Color.White.copy(alpha = 0.65f)),
                    ),
            )
        }

        // Corner overlay, stacked on top of the content above rather than occupying a row of
        // its own: same visual spot as before (inset by the root Box's own 20dp/16dp padding),
        // but it no longer costs the quote+tip block any of the card's vertical space.
        Box(
            modifier = GlanceModifier.fillMaxSize(),
            contentAlignment = Alignment.TopEnd,
        ) {
            // A shaded, ringed circle behind the icon — rather than a bare glyph — so the
            // button reads as tappable at a glance, and so the whole 36dp circle (not just
            // the 20dp glyph inside it) is the actual tap target.
            Box(
                modifier =
                    GlanceModifier
                        .size(36.dp)
                        .cornerRadius(18.dp)
                        .background(ImageProvider(R.drawable.widget_settings_button_bg))
                        .clickable(actionStartActivity(Intent(context, SettingsActivity::class.java))),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_widget_settings),
                    contentDescription = context.getString(R.string.widget_settings_action),
                    modifier = GlanceModifier.size(20.dp),
                )
            }
        }
    }
}
