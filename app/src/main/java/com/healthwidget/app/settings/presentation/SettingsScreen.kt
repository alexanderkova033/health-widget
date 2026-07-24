package com.healthwidget.app.settings.presentation

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.healthwidget.app.HealthWidgetApp
import com.healthwidget.app.R
import com.healthwidget.core.settings.AppSettings
import com.healthwidget.core.settings.SettingsRepository
import com.healthwidget.core.settings.VarietyLevel
import com.healthwidget.core.tips.DayPart
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

        // "Why this tip?" leads (see the placement discussion in commit history): the user
        // orients on the tip they're actually looking at first, then tunes how tips are picked.
        lastTipText?.let { text ->
            TipSourceSection(
                tipText = text,
                source = tipEngine.findByText(text),
                dayPart = tipEngine.dayPartFor(LocalTime.now()),
                onRefresh = { refreshTipNow(context, settings.varietyLevel) },
            )
        }

        SectionCard {
            VarietySection(
                level = settings.varietyLevel,
                onLevelChange = { level -> scope.launch { settingsRepository.setVarietyLevel(level) } },
            )
        }

        SectionCard {
            AboutSection()
        }
    }
}

/** Which [MaterialTheme] color role a day part's accent is drawn from — kept to the three
 * theme roles (rather than bespoke hardcoded hues) so the hero tip card stays in gamut with
 * dynamic color (Material You) when it's active, not just the static fallback palette. */
private enum class AccentRole { PRIMARY, SECONDARY, TERTIARY }

@Composable
private fun AccentRole.accent(): Color =
    when (this) {
        AccentRole.PRIMARY -> MaterialTheme.colorScheme.primary
        AccentRole.SECONDARY -> MaterialTheme.colorScheme.secondary
        AccentRole.TERTIARY -> MaterialTheme.colorScheme.tertiary
    }

@Composable
private fun AccentRole.onAccent(): Color =
    when (this) {
        AccentRole.PRIMARY -> MaterialTheme.colorScheme.onPrimary
        AccentRole.SECONDARY -> MaterialTheme.colorScheme.onSecondary
        AccentRole.TERTIARY -> MaterialTheme.colorScheme.onTertiary
    }

@Composable
private fun AccentRole.container(): Color =
    when (this) {
        AccentRole.PRIMARY -> MaterialTheme.colorScheme.primaryContainer
        AccentRole.SECONDARY -> MaterialTheme.colorScheme.secondaryContainer
        AccentRole.TERTIARY -> MaterialTheme.colorScheme.tertiaryContainer
    }

private data class DayPartVisual(val icon: ImageVector, val label: String, val role: AccentRole)

/** Sleep's two [DayPart] variants ([DayPart.SLEEP_LATE], [DayPart.SLEEP_EARLY_HOURS]) share one
 * "Night" visual — the split only matters to [TipEngine]'s message selection, not to how this
 * card presents itself. */
@Composable
private fun dayPartVisual(dayPart: DayPart): DayPartVisual =
    when (dayPart) {
        DayPart.MORNING ->
            DayPartVisual(
                Icons.Filled.WbTwilight,
                stringResource(R.string.settings_daypart_morning),
                AccentRole.TERTIARY,
            )
        DayPart.AFTERNOON ->
            DayPartVisual(
                Icons.Filled.WbSunny,
                stringResource(R.string.settings_daypart_afternoon),
                AccentRole.SECONDARY,
            )
        DayPart.EVENING ->
            DayPartVisual(
                Icons.Filled.DarkMode,
                stringResource(R.string.settings_daypart_evening),
                AccentRole.PRIMARY,
            )
        DayPart.SLEEP_LATE, DayPart.SLEEP_EARLY_HOURS ->
            DayPartVisual(
                Icons.Filled.Bedtime,
                stringResource(R.string.settings_daypart_night),
                AccentRole.PRIMARY,
            )
    }

/** The variety setting is a lean, not a filter (see [TipEngine.messageFor]'s `varietyLevel`
 * parameter): none of the three levels ever remove the practical tips or the
 * philosophical/lighthearted ones entirely, each just shifts which one is the overwhelming
 * majority of what shows up. */
@Composable
private fun VarietySection(
    level: VarietyLevel,
    onLevelChange: (VarietyLevel) -> Unit,
) {
    SectionTitle(icon = Icons.Filled.Tune, text = stringResource(R.string.settings_variety_title))
    Text(
        text = stringResource(level.stateDescriptionRes()),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Medium,
    )
    Spacer(Modifier.height(12.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        VarietyLevel.entries.forEach { candidate ->
            VarietyLevelChip(
                level = candidate,
                selected = candidate == level,
                onClick = { onLevelChange(candidate) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun VarietyLevelChip(
    level: VarietyLevel,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                ).clickable(onClick = onClick)
                .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(level.labelRes()),
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun VarietyLevel.labelRes(): Int =
    when (this) {
        VarietyLevel.PRACTICAL -> R.string.settings_variety_label_practical
        VarietyLevel.BALANCED -> R.string.settings_variety_label_balanced
        VarietyLevel.PLAYFUL -> R.string.settings_variety_label_playful
    }

private fun VarietyLevel.stateDescriptionRes(): Int =
    when (this) {
        VarietyLevel.PRACTICAL -> R.string.settings_variety_state_practical
        VarietyLevel.BALANCED -> R.string.settings_variety_state_balanced
        VarietyLevel.PLAYFUL -> R.string.settings_variety_state_playful
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
    varietyLevel: VarietyLevel,
) {
    val app = context.applicationContext as HealthWidgetApp
    app.applicationScope.launch {
        app.container.advanceTip(LocalTime.now(), manual = true, varietyLevel = varietyLevel)
        app.container.refreshWidget()
    }
}

/** Shared card chrome for every plain settings section — groups related controls into a clearly
 * bounded, tappable-feeling block instead of a flat list separated by thin dividers. The hero
 * tip card ([TipSourceSection]) draws its own chrome instead of using this, since its whole
 * point is to not look like a plain list row. */
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
 *
 * Tinted by [dayPart] rather than a fixed color: the card is meant to read as "the tip that's on
 * your home screen right now", so its accent follows the same signal ([TipEngine.dayPartFor])
 * that picked that tip in the first place.
 */
@Composable
private fun TipSourceSection(
    tipText: String,
    source: Tip?,
    dayPart: DayPart,
    onRefresh: () -> Unit,
) {
    val visual = dayPartVisual(dayPart)
    val accent = visual.role.accent()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Column(
            modifier =
                Modifier
                    .background(
                        Brush.verticalGradient(
                            listOf(visual.role.container(), MaterialTheme.colorScheme.surfaceContainer),
                        ),
                    )
                    .padding(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier =
                        Modifier
                            .size(40.dp)
                            .background(accent, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = visual.icon,
                        contentDescription = null,
                        tint = visual.role.onAccent(),
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = visual.label.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = accent,
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.settings_tip_source_quote, tipText),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium,
            )
            if (source != null) {
                Spacer(Modifier.height(14.dp))
                val context = LocalContext.current
                Row(
                    modifier = Modifier.clickable { openSource(context, source.sourceUrl) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = source.sourceLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = accent,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            FilledTonalButton(onClick = onRefresh) {
                Icon(
                    imageVector = Icons.Filled.Shuffle,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.settings_tip_refresh_action))
            }
        }
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
        Spacer(Modifier.height(12.dp))
        AboutRow(icon = Icons.Filled.PhoneAndroid, text = stringResource(R.string.settings_about_privacy_local))
        Spacer(Modifier.height(8.dp))
        AboutRow(icon = Icons.Filled.PersonOff, text = stringResource(R.string.settings_about_privacy_account))
        Spacer(Modifier.height(8.dp))
        AboutRow(icon = Icons.Filled.VisibilityOff, text = stringResource(R.string.settings_about_privacy_ads))
    }
}

@Composable
private fun AboutRow(
    icon: ImageVector,
    text: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
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
