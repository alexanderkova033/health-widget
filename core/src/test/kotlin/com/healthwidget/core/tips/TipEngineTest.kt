package com.healthwidget.core.tips

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalTime
import java.util.stream.Stream
import kotlin.random.Random

/** Deterministic [Random] stand-in: every `nextInt(until)` call returns the same fixed index. */
private class FixedIndexRandom(private val index: Int) : Random() {
    override fun nextBits(bitCount: Int): Int = index

    override fun nextInt(until: Int): Int = index
}

private fun tip(text: String) = Tip(text = text, sourceLabel = "Test source", sourceUrl = "https://example.test")

private val testCatalog =
    TipCatalog(
        general = listOf("G1", "G2", "G3", "G4").map(::tip),
        morning = listOf("M1", "M2").map(::tip),
        afternoon = listOf("A1").map(::tip),
        evening = listOf("E1").map(::tip),
        sleepLate = tip("Sleep late fixed message"),
        sleepEarlyHours = tip("Sleep early-hours fixed message"),
    )

class TipEngineTest {
    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("dayPartBoundaries")
    fun `day part boundaries`(
        time: LocalTime,
        expected: DayPart,
    ) {
        val engine = TipEngine(testCatalog, Random.Default)
        assertThat(engine.dayPartFor(time)).isEqualTo(expected)
    }

    @Test
    fun `morning pool is general plus morning-specific tips`() {
        assertPoolComposition(LocalTime.of(9, 0), testCatalog.general + testCatalog.morning)
    }

    @Test
    fun `afternoon pool is general plus afternoon-specific tips`() {
        assertPoolComposition(LocalTime.of(14, 0), testCatalog.general + testCatalog.afternoon)
    }

    @Test
    fun `evening pool is general plus evening-specific tips`() {
        assertPoolComposition(LocalTime.of(20, 0), testCatalog.general + testCatalog.evening)
    }

    @Test
    fun `sleep late message is the fixed catalog message`() {
        val engine = TipEngine(testCatalog, FixedIndexRandom(0))
        val message = engine.messageFor(LocalTime.of(23, 30), recentTips = emptyList())
        assertThat(message).isEqualTo(testCatalog.sleepLate)
    }

    @Test
    fun `sleep early-hours message is the fixed catalog message`() {
        val engine = TipEngine(testCatalog, FixedIndexRandom(0))
        val message = engine.messageFor(LocalTime.of(2, 0), recentTips = emptyList())
        assertThat(message).isEqualTo(testCatalog.sleepEarlyHours)
    }

    @Test
    fun `sleep messages are exempt from anti-repeat`() {
        val engine = TipEngine(testCatalog, FixedIndexRandom(0))
        val time = LocalTime.of(23, 30)
        val first = engine.messageFor(time, recentTips = emptyList())
        val second = engine.messageFor(time, recentTips = listOf(first.text))
        assertThat(second).isEqualTo(first)
        assertThat(second).isEqualTo(testCatalog.sleepLate)
    }

    @Test
    fun `excludes every tip in recentTips, not just the most recent one`() {
        val engine = TipEngine(testCatalog, Random.Default)
        // Morning pool is G1-G4, M1-M2 (6 members); excluding all but M2 must deterministically
        // return M2 regardless of the random source, proving the whole list is honored.
        val recentTips = listOf("G1", "G2", "G3", "G4", "M1")
        val tip = engine.messageFor(LocalTime.of(9, 0), recentTips)
        assertThat(tip.text).isEqualTo("M2")
    }

    @Test
    fun `never repeats a tip within a bounded recent window`() {
        val engine = TipEngine(testCatalog, Random(seed = 42))
        val windowSize = 4
        val window = ArrayDeque<String>()
        repeat(200) {
            val tip = engine.messageFor(LocalTime.of(9, 0), window.toList())
            assertThat(window).doesNotContain(tip.text)
            window.addLast(tip.text)
            if (window.size > windowSize) window.removeFirst()
        }
    }

    @Test
    fun `falls back to a repeat when recentTips covers the entire pool`() {
        val singleTipCatalog =
            testCatalog.copy(general = listOf(tip("Only")), morning = emptyList())
        val engine = TipEngine(singleTipCatalog, FixedIndexRandom(0))

        val tip = engine.messageFor(LocalTime.of(9, 0), recentTips = listOf("Only"))

        assertThat(tip.text).isEqualTo("Only")
    }

    private fun assertPoolComposition(
        time: LocalTime,
        expectedPool: List<Tip>,
    ) {
        val actual =
            expectedPool.indices
                .map { index ->
                    TipEngine(testCatalog, FixedIndexRandom(index)).messageFor(time, recentTips = emptyList())
                }.toSet()
        assertThat(actual).isEqualTo(expectedPool.toSet())
    }

    companion object {
        @JvmStatic
        fun dayPartBoundaries(): Stream<Arguments> =
            Stream.of(
                Arguments.of(LocalTime.of(5, 59), DayPart.SLEEP_EARLY_HOURS),
                Arguments.of(LocalTime.of(6, 0), DayPart.MORNING),
                Arguments.of(LocalTime.of(11, 59), DayPart.MORNING),
                Arguments.of(LocalTime.of(12, 0), DayPart.AFTERNOON),
                Arguments.of(LocalTime.of(17, 59), DayPart.AFTERNOON),
                Arguments.of(LocalTime.of(18, 0), DayPart.EVENING),
                Arguments.of(LocalTime.of(22, 59), DayPart.EVENING),
                Arguments.of(LocalTime.of(23, 0), DayPart.SLEEP_LATE),
                Arguments.of(LocalTime.of(0, 0), DayPart.SLEEP_EARLY_HOURS),
            )
    }
}
