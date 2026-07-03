package com.healthwidget.app

import android.app.Application

class HealthWidgetApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
