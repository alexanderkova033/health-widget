package com.healthwidget.app.settings.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.google.common.truth.Truth.assertThat
import com.healthwidget.core.settings.AppSettings
import com.healthwidget.core.settings.WidgetRefreshInterval
import com.healthwidget.core.settings.WidgetStyle
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.time.LocalTime

class DataStoreSettingsRepositoryTest {
    @TempDir
    lateinit var tempDir: File

    private lateinit var repository: DataStoreSettingsRepository

    @BeforeEach
    fun setUp() {
        val dataStore =
            PreferenceDataStoreFactory.create(
                produceFile = { File(tempDir, "settings.preferences_pb") },
            )
        repository = DataStoreSettingsRepository(dataStore)
    }

    @Test
    fun `defaults are returned when nothing has been written`() =
        runTest {
            assertThat(repository.settings.first()).isEqualTo(AppSettings.DEFAULT)
        }

    @Test
    fun `setNotificationFrequency persists and is reflected in settings flow`() =
        runTest {
            repository.setNotificationFrequency(0)
            assertThat(repository.settings.first().notificationFrequency).isEqualTo(0)

            repository.setNotificationFrequency(6)
            assertThat(repository.settings.first().notificationFrequency).isEqualTo(6)
        }

    @Test
    fun `setNotificationFrequency rejects out-of-range values`() =
        runTest {
            assertThrows<IllegalArgumentException> { repository.setNotificationFrequency(-1) }
            assertThrows<IllegalArgumentException> { repository.setNotificationFrequency(7) }
        }

    @Test
    fun `setSleepAlertEnabled persists`() =
        runTest {
            repository.setSleepAlertEnabled(false)
            assertThat(repository.settings.first().sleepAlertEnabled).isFalse()
        }

    @Test
    fun `setQuietHours persists both bounds`() =
        runTest {
            val start = LocalTime.of(22, 0)
            val end = LocalTime.of(6, 30)
            repository.setQuietHours(start, end)

            val settings = repository.settings.first()
            assertThat(settings.quietHoursStart).isEqualTo(start)
            assertThat(settings.quietHoursEnd).isEqualTo(end)
        }

    @Test
    fun `setWidgetStyle persists and is reflected in settings flow`() =
        runTest {
            repository.setWidgetStyle(WidgetStyle.MIDNIGHT)
            assertThat(repository.settings.first().widgetStyle).isEqualTo(WidgetStyle.MIDNIGHT)
        }

    @Test
    fun `setWidgetRefreshInterval persists and is reflected in settings flow`() =
        runTest {
            repository.setWidgetRefreshInterval(WidgetRefreshInterval.ONE_HOUR)
            assertThat(repository.settings.first().widgetRefreshInterval).isEqualTo(WidgetRefreshInterval.ONE_HOUR)
        }
}
