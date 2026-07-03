package com.healthwidget.app.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.google.common.truth.Truth.assertThat
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class TipHistoryRepositoryTest {
    @TempDir
    lateinit var tempDir: File

    private lateinit var repository: TipHistoryRepository

    @BeforeEach
    fun setUp() {
        val dataStore =
            PreferenceDataStoreFactory.create(
                produceFile = { File(tempDir, "tip_history.preferences_pb") },
            )
        repository = TipHistoryRepository(dataStore)
    }

    @Test
    fun `lastTip is null before anything is stored`() =
        runTest {
            assertThat(repository.lastTip.first()).isNull()
        }

    @Test
    fun `setLastTip persists and is reflected in the flow`() =
        runTest {
            repository.setLastTip("Take a deep breath.")
            assertThat(repository.lastTip.first()).isEqualTo("Take a deep breath.")

            repository.setLastTip("Stand up and stretch.")
            assertThat(repository.lastTip.first()).isEqualTo("Stand up and stretch.")
        }
}
