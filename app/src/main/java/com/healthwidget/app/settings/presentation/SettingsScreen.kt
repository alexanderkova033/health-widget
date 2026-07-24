package com.healthwidget.app.settings.presentation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.glance.appwidget.updateAll
import com.healthwidget.app.R
import com.healthwidget.app.notifications.NudgeScheduler
import com.healthwidget.app.widget.TipWidget
import com.healthwidget.app.widget.WidgetScheduler
import com.healthwidget.core.settings.AppSettings
import com.healthwidget.core.settings.SettingsRepository
import com.healthwidget.core.settings.WidgetRefreshInterval
import com.healthwidget.core.settings.WidgetStyle
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalTime

@Composable
fun SettingsScreen(
    settingsRepository: SettingsRepository,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var settings by remember { mutableStateOf(AppSettings.DEFAULT) }

    LaunchedEffect(settingsRepository) {
        settingsRepository.settings.collectLatest { settings = it }
    }

    var notificationsGranted by rememberSaveable { mutableStateOf(hasNotificationPermission(context)) }
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            notificationsGranted = granted
        }

    fun updateSettings(newSettings: AppSettings) {
        scope.launch {
            if (newSettings.notificationFrequency != settings.notificationFrequency) {
                settingsRepository.setNotificationFrequency(newSettings.notificationFrequency)
            }
            if (newSettings.sleepAlertEnabled != settings.sleepAlertEnabled) {
                settingsRepository.setSleepAlertEnabled(newSettings.sleepAlertEnabled)
            }
            if (newSettings.quietHoursStart != settings.quietHoursStart ||
                newSettings.quietHoursEnd != settings.quietHoursEnd
            ) {
                settingsRepository.setQuietHours(newSettings.quietHoursStart, newSettings.quietHoursEnd)
            }
            if (newSettings.widgetStyle != settings.widgetStyle) {
                settingsRepository.setWidgetStyle(newSettings.widgetStyle)
                TipWidget().updateAll(context)
            }
            if (newSettings.widgetRefreshInterval != settings.widgetRefreshInterval) {
                settingsRepository.setWidgetRefreshInterval(newSettings.widgetRefreshInterval)
                WidgetScheduler(context).rescheduleAll(newSettings.widgetRefreshInterval.minutes)
            }
            NudgeScheduler(context).rescheduleAll(newSettings)
        }
    }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
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
        Spacer(Modifier.height(24.dp))

        if (needsNotificationPermission(settings, notificationsGranted)) {
            NotificationPermissionCard(
                onAllowClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
            )
            Spacer(Modifier.height(24.dp))
        }

        FrequencySection(
            frequency = settings.notificationFrequency,
            onFrequencyChange = { updateSettings(settings.copy(notificationFrequency = it)) },
        )
        Spacer(Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(Modifier.height(24.dp))

        SleepAlertSection(
            enabled = settings.sleepAlertEnabled,
            onEnabledChange = { updateSettings(settings.copy(sleepAlertEnabled = it)) },
        )
        Spacer(Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(Modifier.height(24.dp))

        QuietHoursSection(
            start = settings.quietHoursStart,
            end = settings.quietHoursEnd,
            onChange = { start, end -> updateSettings(settings.copy(quietHoursStart = start, quietHoursEnd = end)) },
        )
        Spacer(Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(Modifier.height(24.dp))

        WidgetSection(
            style = settings.widgetStyle,
            refreshInterval = settings.widgetRefreshInterval,
            onStyleChange = { updateSettings(settings.copy(widgetStyle = it)) },
            onRefreshIntervalChange = { updateSettings(settings.copy(widgetRefreshInterval = it)) },
        )
        Spacer(Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(Modifier.height(24.dp))

        AboutSection()
    }
}

private fun needsNotificationPermission(
    settings: AppSettings,
    granted: Boolean,
): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        !granted &&
        (settings.notificationFrequency > 0 || settings.sleepAlertEnabled)

private fun hasNotificationPermission(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
}

@Composable
private fun NotificationPermissionCard(onAllowClick: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Row(modifier = Modifier.padding(16.dp)) {
            Icon(imageVector = Icons.Filled.Notifications, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = stringResource(R.string.settings_notification_permission_rationale),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onAllowClick) {
                    Text(stringResource(R.string.settings_notification_permission_action))
                }
            }
        }
    }
}

@Composable
private fun FrequencySection(
    frequency: Int,
    onFrequencyChange: (Int) -> Unit,
) {
    // Local drag state re-keyed off the persisted value: lets the thumb move smoothly while
    // dragging without persisting/rescheduling on every intermediate frame (only on release).
    var sliderValue by remember(frequency) { mutableStateOf(frequency.toFloat()) }

    SectionTitle(icon = Icons.Filled.Notifications, text = stringResource(R.string.settings_frequency_title))
    Text(frequencyLabel(sliderValue.toInt()), style = MaterialTheme.typography.bodyLarge)
    Slider(
        value = sliderValue,
        onValueChange = { sliderValue = it },
        onValueChangeFinished = { onFrequencyChange(sliderValue.toInt()) },
        valueRange = AppSettings.MIN_NOTIFICATION_FREQUENCY.toFloat()..AppSettings.MAX_NOTIFICATION_FREQUENCY.toFloat(),
        steps = AppSettings.MAX_NOTIFICATION_FREQUENCY - AppSettings.MIN_NOTIFICATION_FREQUENCY - 1,
    )
}

@Composable
private fun frequencyLabel(frequency: Int): String =
    if (frequency == 0) {
        stringResource(R.string.settings_frequency_off)
    } else {
        pluralStringResource(R.plurals.settings_frequency_times, frequency, frequency)
    }

@Composable
private fun SleepAlertSection(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
) {
    SectionTitle(icon = Icons.Filled.Bedtime, text = stringResource(R.string.settings_sleep_alert_title))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.settings_sleep_alert_description),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(16.dp))
        Switch(checked = enabled, onCheckedChange = onEnabledChange)
    }
}

@Composable
private fun QuietHoursSection(
    start: LocalTime,
    end: LocalTime,
    onChange: (LocalTime, LocalTime) -> Unit,
) {
    var editing by remember { mutableStateOf<QuietHoursField?>(null) }

    SectionTitle(icon = Icons.Filled.Schedule, text = stringResource(R.string.settings_quiet_hours_title))
    Text(stringResource(R.string.settings_quiet_hours_description), style = MaterialTheme.typography.bodyMedium)
    Spacer(Modifier.height(8.dp))
    Row {
        TimeField(
            label = stringResource(R.string.settings_quiet_hours_start),
            time = start,
            onClick = { editing = QuietHoursField.START },
        )
        Spacer(Modifier.width(24.dp))
        TimeField(
            label = stringResource(R.string.settings_quiet_hours_end),
            time = end,
            onClick = { editing = QuietHoursField.END },
        )
    }

    when (editing) {
        QuietHoursField.START ->
            TimePickerDialog(
                initialTime = start,
                onDismiss = { editing = null },
                onConfirm = {
                    onChange(it, end)
                    editing = null
                },
            )
        QuietHoursField.END ->
            TimePickerDialog(
                initialTime = end,
                onDismiss = { editing = null },
                onConfirm = {
                    onChange(start, it)
                    editing = null
                },
            )
        null -> Unit
    }
}

private enum class QuietHoursField { START, END }

@Composable
private fun TimeField(
    label: String,
    time: LocalTime,
    onClick: () -> Unit,
) {
    TextButton(onClick = onClick) {
        Column {
            Text(text = label, style = MaterialTheme.typography.bodySmall)
            Text(text = "%02d:%02d".format(time.hour, time.minute), style = MaterialTheme.typography.titleLarge)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initialTime: LocalTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit,
) {
    val state =
        rememberTimePickerState(
            initialHour = initialTime.hour,
            initialMinute = initialTime.minute,
            is24Hour = true,
        )
    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column(modifier = Modifier.padding(24.dp)) {
                TimePicker(state = state)
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = { onConfirm(LocalTime.of(state.hour, state.minute)) }) {
                        Text(stringResource(R.string.action_ok))
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetSection(
    style: WidgetStyle,
    refreshInterval: WidgetRefreshInterval,
    onStyleChange: (WidgetStyle) -> Unit,
    onRefreshIntervalChange: (WidgetRefreshInterval) -> Unit,
) {
    SectionTitle(icon = Icons.Filled.Widgets, text = stringResource(R.string.settings_widget_title))
    WidgetPreview(style)
    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        WidgetStyle.entries.forEach { candidate ->
            StyleSwatch(
                style = candidate,
                label = candidate.label(),
                selected = candidate == style,
                onClick = { onStyleChange(candidate) },
            )
        }
    }
    Spacer(Modifier.height(20.dp))
    Text(stringResource(R.string.settings_widget_refresh_title), style = MaterialTheme.typography.bodyMedium)
    Spacer(Modifier.height(8.dp))
    RefreshIntervalRow(selected = refreshInterval, onSelect = onRefreshIntervalChange)
}

@Composable
private fun WidgetPreview(style: WidgetStyle) {
    val (start, end) = style.previewColors()
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(110.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.linearGradient(listOf(start, end))),
        contentAlignment = Alignment.Center,
    ) {
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
    val (start, end) = style.previewColors()
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier =
                Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(start, end)))
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

@Composable
private fun WidgetStyle.previewColors(): Pair<Color, Color> =
    when (this) {
        WidgetStyle.FOREST -> colorResource(R.color.widget_gradient_start) to colorResource(R.color.widget_gradient_end)
        WidgetStyle.OCEAN -> colorResource(R.color.widget_ocean_start) to colorResource(R.color.widget_ocean_end)
        WidgetStyle.SUNSET -> colorResource(R.color.widget_sunset_start) to colorResource(R.color.widget_sunset_end)
        WidgetStyle.MIDNIGHT ->
            colorResource(R.color.widget_midnight_start) to colorResource(R.color.widget_midnight_end)
    }

@Composable
private fun WidgetStyle.label(): String =
    when (this) {
        WidgetStyle.FOREST -> stringResource(R.string.settings_widget_style_forest)
        WidgetStyle.OCEAN -> stringResource(R.string.settings_widget_style_ocean)
        WidgetStyle.SUNSET -> stringResource(R.string.settings_widget_style_sunset)
        WidgetStyle.MIDNIGHT -> stringResource(R.string.settings_widget_style_midnight)
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RefreshIntervalRow(
    selected: WidgetRefreshInterval,
    onSelect: (WidgetRefreshInterval) -> Unit,
) {
    val options = WidgetRefreshInterval.entries
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                label = { Text(refreshIntervalLabel(option)) },
            )
        }
    }
}

@Composable
private fun refreshIntervalLabel(interval: WidgetRefreshInterval): String =
    when (interval) {
        WidgetRefreshInterval.ONE_HOUR -> stringResource(R.string.settings_widget_refresh_1h)
        WidgetRefreshInterval.TWO_HOURS -> stringResource(R.string.settings_widget_refresh_2h)
        WidgetRefreshInterval.FOUR_HOURS -> stringResource(R.string.settings_widget_refresh_4h)
    }

@Composable
private fun AboutSection() {
    SectionTitle(icon = Icons.Filled.Shield, text = stringResource(R.string.settings_about_title))
    Text(
        text = stringResource(R.string.settings_about_privacy_promise),
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun SectionTitle(
    icon: ImageVector,
    text: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Text(text = text, style = MaterialTheme.typography.titleLarge)
    }
    Spacer(Modifier.height(8.dp))
}
