package com.healthwidget.app.data

import java.time.LocalTime

/**
 * User-configurable settings (FR6). Everything here lives only in on-device DataStore —
 * nothing is ever transmitted anywhere.
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
