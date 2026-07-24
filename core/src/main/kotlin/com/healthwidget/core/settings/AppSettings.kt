package com.healthwidget.core.settings

/**
 * User-configurable settings (FR6). A plain domain model — persistence is an implementation
 * detail of whatever [SettingsRepository] is backing it.
 *
 * [moreVarietyEnabled] biases tip selection towards the philosophical/lighthearted tone pools
 * rather than switching them on or off outright — see [com.healthwidget.core.tips.TipEngine]'s
 * `pick` for the actual weighting. Off by default: the practical wellness tips are the app's
 * core and stay the overwhelming majority until a user opts into more variety.
 */
data class AppSettings(
    val moreVarietyEnabled: Boolean,
) {
    companion object {
        val DEFAULT = AppSettings(moreVarietyEnabled = false)
    }
}
