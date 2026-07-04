package com.healthwidget.app.settings.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.healthwidget.app.HealthWidgetApp
import com.healthwidget.app.theme.HealthWidgetTheme

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settingsRepository = (application as HealthWidgetApp).container.settingsRepository
        setContent {
            HealthWidgetTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SettingsScreen(settingsRepository = settingsRepository)
                }
            }
        }
    }
}
