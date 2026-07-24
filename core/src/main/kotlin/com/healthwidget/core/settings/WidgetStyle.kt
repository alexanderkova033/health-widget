package com.healthwidget.core.settings

/**
 * Which background style the home-screen widget renders with. Purely a symbolic choice here —
 * mapping a style to an actual drawable/gradient is `:app`'s job (this module has no Android
 * resource concept).
 */
enum class WidgetStyle { FOREST, OCEAN, SUNSET, MIDNIGHT }
