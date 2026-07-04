package com.healthwidget.core.tips

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.time.LocalTime
import kotlin.random.Random

private class FakeTipHistoryRepository(initial: String? = null) : TipHistoryRepository {
    private val state = MutableStateFlow(initial)
    override val lastTip: Flow<String?> = state

    override suspend fun setLastTip(tip: String) {
        state.value = tip
    }
}

class AdvanceTipUseCaseTest {
    private val catalog =
        TipCatalog(
            general = listOf("G1", "G2"),
            morning = listOf("M1"),
            afternoon = emptyList(),
            evening = emptyList(),
            sleepLate = "Sleep late",
            sleepEarlyHours = "Sleep early",
        )

    @Test
    fun `persists the picked tip so it becomes the next lastTip`() =
        runTest {
            val repository = FakeTipHistoryRepository()
            val advanceTip = AdvanceTipUseCase(TipEngine(catalog, Random(seed = 1)), repository)

            val tip = advanceTip(LocalTime.of(9, 0))

            assertThat(repository.lastTip.first()).isEqualTo(tip)
        }

    @Test
    fun `never repeats the previously persisted tip`() =
        runTest {
            val repository = FakeTipHistoryRepository(initial = "G1")
            val advanceTip = AdvanceTipUseCase(TipEngine(catalog, Random(seed = 2)), repository)

            val tip = advanceTip(LocalTime.of(9, 0))

            assertThat(tip).isNotEqualTo("G1")
        }
}
