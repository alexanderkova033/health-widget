package com.healthwidget.app.settings.presentation

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.ImageView
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.healthwidget.app.HealthWidgetApp
import com.healthwidget.app.R
import com.healthwidget.app.widget.backgroundDrawableRes
import com.healthwidget.core.settings.AppSettings
import com.healthwidget.core.settings.SettingsRepository
import com.healthwidget.core.settings.WidgetStyle
import com.healthwidget.core.tips.Tip
import com.healthwidget.core.tips.TipEngine
import com.healthwidget.core.tips.TipHistoryRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalTime

@Composable
fun SettingsScreen(
    settingsRepository: SettingsRepository,
    tipHistoryRepository: TipHistoryRepository,
    tipEngine: TipEngine,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var settings by remember { mutableStateOf(AppSettings.DEFAULT) }
    var lastTipText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(settingsRepository) {
        settingsRepository.settings.collectLatest { settings = it }
    }

    LaunchedEffect(tipHistoryRepository) {
        tipHistoryRepository.recentTips.collectLatest { recent ->
            lastTipText = recent.lastOrNull()
        }
    }

    fun changeWidgetStyle(style: WidgetStyle) {
        scope.launch {
            settingsRepository.setWidgetStyle(style)
            refreshWidgetNow(context)
        }
    }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(text = stringResource(R.string.settings_title), style = MaterialTheme.typography.titleLarge)
        }

        SectionCard {
            WidgetSection(
                style = settings.widgetStyle,
                onStyleChange = { changeWidgetStyle(it) },
            )
        }

        lastTipText?.let { text ->
            SectionCard {
                TipSourceSection(
                    tipText = text,
                    source = tipEngine.findByText(text),
                    onRefresh = { refreshTipNow(context) },
                )
            }
        }

        SectionCard {
            AboutSection()
        }
    }
}

/**
 * Re-renders the widget right away (new style, or a manually refreshed tip). Launched in
 * [HealthWidgetApp.applicationScope] rather than this screen's own coroutine scope: the
 * latter is cancelled if the user navigates away before the Glance composition finishes,
 * which previously left the widget's Glance session stuck until the app was force-restarted.
 * Also avoids the multi-second lag of routing through a WorkManager job for what the user
 * expects to see change instantly. Goes through [com.healthwidget.app.AppContainer.refreshWidget]
 * (not `TipWidget().updateAll()` directly) so this can't race the periodic tick worker or the
 * widget's own tap-to-refresh action and leave a stale render on screen.
 */
private fun refreshWidgetNow(context: Context) {
    val app = context.applicationContext as HealthWidgetApp
    app.applicationScope.launch {
        app.container.refreshWidget()
    }
}

/** Picks a new tip out of turn (same selection/anti-repeat logic as the scheduled refresh)
 * and pushes it to the widget immediately. */
private fun refreshTipNow(context: Context) {
    val app = context.applicationContext as HealthWidgetApp
    app.applicationScope.launch {
        app.container.advanceTip(LocalTime.now())
        app.container.refreshWidget()
    }
}

/** Shared card chrome for every settings section — groups related controls into a clearly
 * bounded, tappable-feeling block instead of a flat list separated by thin dividers. */
@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(20.dp), content = content)
    }
}

@Composable
private fun WidgetSection(
    style: WidgetStyle,
    onStyleChange: (WidgetStyle) -> Unit,
) {
    SectionTitle(icon = Icons.Filled.Widgets, text = stringResource(R.string.settings_widget_title))
    WidgetPreview(style)
    Spacer(Modifier.height(16.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        WidgetStyle.entries.forEach { candidate ->
            StyleSwatch(
                style = candidate,
                label = candidate.label(),
                selected = candidate == style,
                onClick = { onStyleChange(candidate) },
            )
        }
    }
}

/** Renders the real widget background drawable rather than a hand-picked two-colour brush, so
 * this preview never drifts from what actually ends up on the home screen. */
@Composable
private fun WidgetPreview(style: WidgetStyle) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(120.dp)
                .shadow(elevation = 6.dp, shape = RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center,
    ) {
        WidgetBackgroundImage(style, modifier = Modifier.matchParentSize())
        Text(
            text = stringResource(R.string.settings_widget_preview_tip),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
    }
}

@Composable
private fun StyleSwatch(
    style: WidgetStyle,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier =
                Modifier
                    .size(56.dp)
                    .shadow(elevation = if (selected) 4.dp else 0.dp, shape = CircleShape)
                    .clip(CircleShape)
                    .border(
                        width = if (selected) 3.dp else 1.dp,
                        color =
                            if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            },
                        shape = CircleShape,
                    ).clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            WidgetBackgroundImage(style, modifier = Modifier.matchParentSize())
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall)
    }
}

/** [style]'s actual widget background drawable (a layered gradient `layer-list`, not a plain
 * two-stop gradient) via classic View interop — Compose's own `painterResource` only handles
 * vector/raster drawables, not `<layer-list>`/`<shape>` resources. */
@Composable
private fun WidgetBackgroundImage(
    style: WidgetStyle,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        factory = { ctx -> ImageView(ctx).apply { scaleType = ImageView.ScaleType.CENTER_CROP } },
        update = { it.setImageResource(style.backgroundDrawableRes()) },
        modifier = modifier,
    )
}

@Composable
private fun WidgetStyle.label(): String =
    when (this) {
        WidgetStyle.FOREST -> stringResource(R.string.settings_widget_style_forest)
        WidgetStyle.OCEAN -> stringResource(R.string.settings_widget_style_ocean)
        WidgetStyle.SUNSET -> stringResource(R.string.settings_widget_style_sunset)
        WidgetStyle.MIDNIGHT -> stringResource(R.string.settings_widget_style_midnight)
    }

/**
 * [source] is a best-effort lookup ([TipEngine.findByText] matching the persisted plain-text
 * history against the live catalog) and can be null — e.g. right after a wording edit to the
 * tip catalog, the last tip a user was shown may no longer match any current entry
 * byte-for-byte. The card (and the refresh button) must still show in that case: only the
 * citation half depends on a successful match, not the card's existence.
 */
@Composable
private fun TipSourceSection(
    tipText: String,
    source: Tip?,
    onRefresh: () -> Unit,
) {
    SectionTitle(icon = Icons.Filled.Science, text = stringResource(R.string.settings_tip_source_title))
    Text(
        text = stringResource(R.string.settings_tip_source_quote, tipText),
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Medium,
    )
    Spacer(Modifier.height(8.dp))
    if (source != null) {
        Text(text = source.sourceLabel, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
        val context = LocalContext.current
        TextButton(onClick = { openSource(context, source.sourceUrl) }) {
            Text(stringResource(R.string.settings_tip_source_action))
        }
        Text(
            text = stringResource(R.string.settings_tip_source_hint),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
    }
    TextButton(onClick = onRefresh) {
        Icon(
            imageVector = Icons.Filled.Refresh,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(stringResource(R.string.settings_tip_refresh_action))
    }
}

/** Hands off to the system browser; no INTERNET permission needed since the browser process,
 * not this app, makes the request — see the "100% offline" note atop AndroidManifest.xml. */
private fun openSource(
    context: Context,
    url: String,
) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: ActivityNotFoundException) {
        // No browser available on this device — nothing sensible to do, so skip silently.
    }
}

/** Collapsed by default: this is boilerplate/legal-ish content the user rarely needs to
 * revisit, so it shouldn't cost permanent scroll space on every settings visit. */
@Composable
private fun AboutSection() {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Filled.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text(text = stringResource(R.string.settings_about_title), style = MaterialTheme.typography.titleLarge)
        }
        Icon(
            imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
            contentDescription = null,
        )
    }

    if (expanded) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.settings_about_privacy_promise),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun SectionTitle(
    icon: ImageVector,
    text: String,
    trailing: @Composable () -> Unit = {},
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text(text = text, style = MaterialTheme.typography.titleLarge)
        }
        trailing()
    }
    Spacer(Modifier.height(8.dp))
}
