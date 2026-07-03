package com.healthwidget.core

import com.google.common.truth.Truth.assertThat
import java.time.LocalTime
import java.util.stream.Stream
import kotlin.random.Random
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

/** Deterministic [Random] stand-in: every `nextInt(until)` call returns the same fixed index. */
private class FixedIndexRandom(private val index: Int) : Random() {
    override fun nextBits(bitCount: Int): Int = index

    override fun nextInt(until: Int): Int = index
}

private val testCatalog =
    TipCatalog(
        general = listOf("G1", "G2"),
        morning = listOf("M1"),
        afternoon = listOf("A1"),
        evening = listOf("E1"),
        sleepLate = "Sleep late fixed message",
        sleepEarlyHours = "Sleep early-hours fixed message",
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
        val message = engine.messageFor(LocalTime.of(23, 30), lastTip = null)
        assertThat(message).isEqualTo(testCatalog.sleepLate)
    }

    @Test
    fun `sleep early-hours message is the fixed catalog message`() {
        val engine = TipEngine(testCatalog, FixedIndexRandom(0))
        val message = engine.messageFor(LocalTime.of(2, 0), lastTip = null)
        assertThat(message).isEqualTo(testCatalog.sleepEarlyHours)
    }

    @Test
    fun `sleep messages are exempt from anti-repeat`() {
        val engine = TipEngine(testCatalog, FixedIndexRandom(0))
        val time = LocalTime.of(23, 30)
        val first = engine.messageFor(time, lastTip = null)
        val second = engine.messageFor(time, lastTip = first)
        assertThat(second).isEqualTo(first)
        assertThat(second).isEqualTo(testCatalog.sleepLate)
    }

    @Test
    fun `never repeats the last tip when the pool has multiple members`() {
        val engine = TipEngine(testCatalog, Random(seed = 42))
        var lastTip: String? = null
        repeat(200) {
            val tip = engine.messageFor(LocalTime.of(9, 0), lastTip)
            assertThat(tip).isNotEqualTo(lastTip)
            lastTip = tip
        }
    }

    @Test
    fun `falls back to repeating the same tip when the pool has only one member`() {
        val singleTipCatalog =
            testCatalog.copy(general = listOf("Only"), morning = emptyList())
        val engine = TipEngine(singleTipCatalog, FixedIndexRandom(0))

        val tip = engine.messageFor(LocalTime.of(9, 0), lastTip = "Only")

        assertThat(tip).isEqualTo("Only")
    }

    private fun assertPoolComposition(
        time: LocalTime,
        expectedPool: List<String>,
    ) {
        val actual =
            expectedPool.indices
                .map { index -> TipEngine(testCatalog, FixedIndexRandom(index)).messageFor(time, lastTip = null) }
                .toSet()
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
