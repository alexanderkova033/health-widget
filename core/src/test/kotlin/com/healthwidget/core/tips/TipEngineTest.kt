package com.healthwidget.core.tips

import com.google.common.truth.Truth.assertThat
import com.healthwidget.core.settings.VarietyLevel
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
    fun `manual sleep-late request draws from the general pool instead of the fixed message`() {
        val engine = TipEngine(testCatalog, FixedIndexRandom(0))
        val message = engine.messageFor(LocalTime.of(23, 30), recentTips = emptyList(), manual = true)
        assertThat(message).isEqualTo(testCatalog.general[0])
    }

    @Test
    fun `manual sleep-early-hours request draws from the general pool instead of the fixed message`() {
        val engine = TipEngine(testCatalog, FixedIndexRandom(0))
        val message = engine.messageFor(LocalTime.of(2, 0), recentTips = emptyList(), manual = true)
        assertThat(message).isEqualTo(testCatalog.general[0])
    }

    @Test
    fun `manual sleep-hours request still honors anti-repeat`() {
        val engine = TipEngine(testCatalog, Random.Default)
        val recentTips = listOf("G1", "G2", "G3")
        val message = engine.messageFor(LocalTime.of(23, 30), recentTips, manual = true)
        assertThat(message.text).isEqualTo("G4")
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

    @Test
    fun `variety level is a no-op while the tone pools are empty`() {
        // testCatalog has no philosophical/lighthearted content (the toggle's foundation, not
        // yet wired to any real content) — varietyLevel must not change anything while that's
        // true, since selectGroup should short-circuit straight to the practical pool.
        assertPoolComposition(
            LocalTime.of(9, 0),
            testCatalog.general + testCatalog.morning,
            varietyLevel = VarietyLevel.PLAYFUL,
        )
    }

    @Test
    fun `PLAYFUL makes the tone pool the overwhelming majority, not the only option`() {
        val toneCatalog = testCatalog.copy(philosophical = listOf(tip("P1"), tip("P2")))
        val engine = TipEngine(toneCatalog, Random(seed = 7))

        val toneShare = toneShareOverManyDraws(engine, VarietyLevel.PLAYFUL)

        // 80% target (TONE_DOMINANT_CHANCE_PERCENT) with tolerance for statistical noise -
        // the point of this test is "clearly dominant," not "exactly 80%."
        assertThat(toneShare).isAtLeast(0.65)
        assertThat(toneShare).isAtMost(0.95)
    }

    @Test
    fun `PRACTICAL still lets the tone pool through sometimes, not never`() {
        val toneCatalog = testCatalog.copy(philosophical = listOf(tip("P1"), tip("P2")))
        val engine = TipEngine(toneCatalog, Random(seed = 7))

        val toneShare = toneShareOverManyDraws(engine, VarietyLevel.PRACTICAL)

        // 20% target (TONE_MINORITY_CHANCE_PERCENT) with the same generous tolerance.
        assertThat(toneShare).isAtLeast(0.05)
        assertThat(toneShare).isAtMost(0.35)
    }

    @Test
    fun `BALANCED sits roughly at an even split`() {
        val toneCatalog = testCatalog.copy(philosophical = listOf(tip("P1"), tip("P2")))
        val engine = TipEngine(toneCatalog, Random(seed = 7))

        val toneShare = toneShareOverManyDraws(engine, VarietyLevel.BALANCED)

        // 50% target (TONE_BALANCED_CHANCE_PERCENT) with the same generous tolerance.
        assertThat(toneShare).isAtLeast(0.35)
        assertThat(toneShare).isAtMost(0.65)
    }

    @Test
    fun `falls back to the practical pool if the tone pool is empty, even at PLAYFUL`() {
        val engine = TipEngine(testCatalog, Random(seed = 1))
        repeat(50) {
            val tip =
                engine.messageFor(LocalTime.of(9, 0), recentTips = emptyList(), varietyLevel = VarietyLevel.PLAYFUL)
            assertThat(testCatalog.general + testCatalog.morning).contains(tip)
        }
    }

    @Test
    fun `falls back to the tone pool if the practical pool is empty`() {
        val emptyGeneralCatalog =
            testCatalog.copy(
                general = emptyList(),
                morning = emptyList(),
                philosophical = listOf(tip("P1")),
            )
        val engine = TipEngine(emptyGeneralCatalog, FixedIndexRandom(0))

        val tip = engine.messageFor(LocalTime.of(9, 0), recentTips = emptyList(), varietyLevel = VarietyLevel.PRACTICAL)

        assertThat(tip.text).isEqualTo("P1")
    }

    @Test
    fun `prefers an unseen tip from the other group over repeating within the weighted group`() {
        // Single-tip tone pool, just shown - repeating it would be an unforced anti-repeat
        // violation while G1-G4/M1/M2 sit fresh and unused right next to it. PLAYFUL would
        // normally make the tone pool the 80% favorite, but with nothing unseen left in it, the
        // practical pool's fresh tips must win regardless of that weighting - Random.Default is
        // fine here since the fix makes this branch deterministic (no coin flip happens once
        // one side has no fresh candidates).
        val catalog = testCatalog.copy(philosophical = listOf(tip("P1")))
        val engine = TipEngine(catalog, Random.Default)

        val tip = engine.messageFor(LocalTime.of(9, 0), recentTips = listOf("P1"), varietyLevel = VarietyLevel.PLAYFUL)

        assertThat(tip.text).isNotEqualTo("P1")
        assertThat(catalog.general + catalog.morning).contains(tip)
    }

    @Test
    fun `falls back to a weighted repeat when both groups are fully exhausted`() {
        val catalog = testCatalog.copy(philosophical = listOf(tip("P1")))
        val allShown = (catalog.general + catalog.morning).map { it.text } + listOf("P1")
        val engine = TipEngine(catalog, FixedIndexRandom(0))

        val tip = engine.messageFor(LocalTime.of(9, 0), recentTips = allShown, varietyLevel = VarietyLevel.PRACTICAL)

        // Nothing is actually unseen any more; this just shouldn't crash, and whatever comes
        // back must still be a real catalog tip.
        assertThat(catalog.general + catalog.morning + catalog.philosophical).contains(tip)
    }

    private fun toneShareOverManyDraws(
        engine: TipEngine,
        varietyLevel: VarietyLevel,
        draws: Int = 2000,
    ): Double {
        val toneHits =
            (1..draws).count {
                val tip =
                    engine.messageFor(
                        LocalTime.of(9, 0),
                        recentTips = emptyList(),
                        varietyLevel = varietyLevel,
                    )
                tip.text.startsWith("P")
            }
        return toneHits.toDouble() / draws
    }

    private fun assertPoolComposition(
        time: LocalTime,
        expectedPool: List<Tip>,
        varietyLevel: VarietyLevel = VarietyLevel.PRACTICAL,
    ) {
        val actual =
            expectedPool.indices
                .map { index ->
                    TipEngine(testCatalog, FixedIndexRandom(index))
                        .messageFor(time, recentTips = emptyList(), varietyLevel = varietyLevel)
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
