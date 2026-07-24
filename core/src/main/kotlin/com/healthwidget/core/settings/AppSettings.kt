package com.healthwidget.core.settings

/**
 * User-configurable settings (FR6): widget appearance. A plain domain model — persistence is
 * an implementation detail of whatever [SettingsRepository] is backing it.
 */
data class AppSettings(
    val widgetStyle: WidgetStyle,
) {
    companion object {
        val DEFAULT = AppSettings(widgetStyle = WidgetStyle.FOREST)
    }
}
