package com.healthwidget.core.tips

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.time.LocalTime
import kotlin.random.Random

private class FakeTipHistoryRepository(initial: List<String> = emptyList()) : TipHistoryRepository {
    private val state = MutableStateFlow(initial)
    override val recentTips: Flow<List<String>> = state

    override suspend fun recordTip(tip: String) {
        state.value = (state.value + tip).takeLast(TipHistoryRepository.MAX_RECENT_TIPS)
    }
}

private fun tip(text: String) = Tip(text = text, sourceLabel = "Test source", sourceUrl = "https://example.test")

class AdvanceTipUseCaseTest {
    private val catalog =
        TipCatalog(
            general = listOf("G1", "G2").map(::tip),
            morning = listOf("M1").map(::tip),
            afternoon = emptyList(),
            evening = emptyList(),
            sleepLate = tip("Sleep late"),
            sleepEarlyHours = tip("Sleep early"),
        )

    @Test
    fun `persists the picked tip so it appears in recentTips`() =
        runTest {
            val repository = FakeTipHistoryRepository()
            val advanceTip = AdvanceTipUseCase(TipEngine(catalog, Random(seed = 1)), repository)

            val tip = advanceTip(LocalTime.of(9, 0))

            assertThat(repository.recentTips.first()).contains(tip.text)
        }

    @Test
    fun `never repeats a tip already in the recent history`() =
        runTest {
            val repository = FakeTipHistoryRepository(initial = listOf("G1"))
            val advanceTip = AdvanceTipUseCase(TipEngine(catalog, Random(seed = 2)), repository)

            val tip = advanceTip(LocalTime.of(9, 0))

            assertThat(tip.text).isNotEqualTo("G1")
        }
}
