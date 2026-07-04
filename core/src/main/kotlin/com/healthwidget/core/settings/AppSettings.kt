package com.healthwidget.core.settings

import java.time.LocalTime

/**
 * User-configurable settings (FR6): notification frequency, sleep alert toggle, and quiet
 * hours. A plain domain model — persistence is an implementation detail of whatever
 * [SettingsRepository] is backing it.
 */
data class AppSettings(
    val notificationFrequency: Int,
    val sleepAlertEnabled: Boolean,
    val quietHoursStart: LocalTime,
    val quietHoursEnd: LocalTime,
) {
    companion object {
        const val MIN_NOTIFICATION_FREQUENCY = 0
        const val MAX_NOTIFICATION_FREQUENCY = 3

        val DEFAULT =
            AppSettings(
                notificationFrequency = 2,
                sleepAlertEnabled = true,
                quietHoursStart = LocalTime.of(23, 30),
                quietHoursEnd = LocalTime.of(7, 0),
            )
    }
}
