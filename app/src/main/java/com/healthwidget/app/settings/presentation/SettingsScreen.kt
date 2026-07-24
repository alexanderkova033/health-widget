package com.healthwidget.app.settings.presentation

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.healthwidget.app.HealthWidgetApp
import com.healthwidget.app.R
import com.healthwidget.core.settings.AppSettings
import com.healthwidget.core.settings.SettingsRepository
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
            VarietySection(
                enabled = settings.moreVarietyEnabled,
                onToggle = { enabled -> scope.launch { settingsRepository.setMoreVarietyEnabled(enabled) } },
            )
        }

        lastTipText?.let { text ->
            SectionCard {
                TipSourceSection(
                    tipText = text,
                    source = tipEngine.findByText(text),
                    onRefresh = { refreshTipNow(context, settings.moreVarietyEnabled) },
                )
            }
        }

        SectionCard {
            AboutSection()
        }
    }
}

/** The "more variety" toggle — a lean, not a filter (see [TipEngine.messageFor]'s
 * `moreVarietyEnabled` parameter): it never removes the practical tips or the
 * philosophical/lighthearted ones entirely, it just flips which one is the overwhelming
 * majority of what shows up. */
@Composable
private fun VarietySection(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    SectionTitle(
        icon = Icons.Filled.Shuffle,
        text = stringResource(R.string.settings_variety_title),
        trailing = { Switch(checked = enabled, onCheckedChange = onToggle) },
    )
    Text(
        text = stringResource(R.string.settings_variety_description),
        style = MaterialTheme.typography.bodyMedium,
    )
}

/** Picks a new tip out of turn (same selection/anti-repeat logic as the scheduled refresh —
 * see [com.healthwidget.core.tips.TipEngine.messageFor]'s `manual` parameter) and pushes it,
 * plus the background style that now follows it, to the widget immediately. Launched in
 * [HealthWidgetApp.applicationScope] rather than this screen's own coroutine scope: the
 * latter is cancelled if the user navigates away before the Glance composition finishes,
 * which previously left the widget's Glance session stuck until the app was force-restarted.
 * Goes through [com.healthwidget.app.AppContainer.refreshWidget] (not `TipWidget().updateAll()`
 * directly) so this can't race the periodic tick worker or the widget's own tap-to-refresh
 * action and leave a stale render on screen.
 */
private fun refreshTipNow(
    context: Context,
    moreVarietyEnabled: Boolean,
) {
    val app = context.applicationContext as HealthWidgetApp
    app.applicationScope.launch {
        app.container.advanceTip(LocalTime.now(), manual = true, moreVarietyEnabled = moreVarietyEnabled)
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
