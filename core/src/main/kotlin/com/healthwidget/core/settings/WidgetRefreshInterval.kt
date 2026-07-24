package com.healthwidget.core.settings

/**
 * How often the widget refreshes to a new tip (FR1 requires at least every 2 hours, so
 * [TWO_HOURS] stays the default — [ONE_HOUR] and [FOUR_HOURS] are user-chosen alternatives).
 */
enum class WidgetRefreshInterval(val minutes: Long) {
    ONE_HOUR(60L),
    TWO_HOURS(120L),
    FOUR_HOURS(240L),
}
