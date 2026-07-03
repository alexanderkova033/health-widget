package com.healthwidget.app.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.time.LocalTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir

class SettingsRepositoryTest {
    @TempDir
    lateinit var tempDir: File

    private lateinit var repository: SettingsRepository

    @BeforeEach
    fun setUp() {
        val dataStore =
            PreferenceDataStoreFactory.create(
                produceFile = { File(tempDir, "settings.preferences_pb") },
            )
        repository = SettingsRepository(dataStore)
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

            repository.setNotificationFrequency(3)
            assertThat(repository.settings.first().notificationFrequency).isEqualTo(3)
        }

    @Test
    fun `setNotificationFrequency rejects out-of-range values`() =
        runTest {
            assertThrows<IllegalArgumentException> { repository.setNotificationFrequency(-1) }
            assertThrows<IllegalArgumentException> { repository.setNotificationFrequency(4) }
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
}
