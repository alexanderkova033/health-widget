package com.healthwidget.core.settings

import java.time.LocalTime

/**
 * User-configurable settings (FR6): notification frequency, sleep alert toggle, quiet hours,
 * and widget appearance. A plain domain model — persistence is an implementation detail of
 * whatever [SettingsRepository] is backing it.
 */
data class AppSettings(
    val notificationFrequency: Int,
    val sleepAlertEnabled: Boolean,
    val quietHoursStart: LocalTime,
    val quietHoursEnd: LocalTime,
    val widgetStyle: WidgetStyle,
) {
    companion object {
        const val MIN_NOTIFICATION_FREQUENCY = 0

        // Bumped from the original 3/day cap by request — still fixed default times per
        // level (see NudgeScheduler.NUDGE_TIMES_BY_FREQUENCY), not a full custom-time picker.
        const val MAX_NOTIFICATION_FREQUENCY = 6

        val DEFAULT =
            AppSettings(
                notificationFrequency = 2,
                sleepAlertEnabled = true,
                quietHoursStart = LocalTime.of(23, 30),
                quietHoursEnd = LocalTime.of(7, 0),
                widgetStyle = WidgetStyle.FOREST,
            )
    }
}
