package com.healthwidget.core.settings

/**
 * How strongly tip selection leans towards the philosophical/lighthearted tone pools rather
 * than the practical one — never a filter, see [com.healthwidget.core.tips.TipEngine]'s `pick`
 * for the actual weighting each level maps to.
 */
enum class VarietyLevel {
    PRACTICAL,
    BALANCED,
    PLAYFUL,
}
