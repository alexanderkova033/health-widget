package com.healthwidget.core

import com.google.common.truth.Truth.assertThat
import java.time.LocalTime
import java.util.stream.Stream
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class QuietHoursTest {
    @ParameterizedTest(name = "{0} within [{1}, {2}) -> {3}")
    @MethodSource("wrappingWindowCases")
    fun `default wrapping window 23-30 to 07-00`(
        now: LocalTime,
        start: LocalTime,
        end: LocalTime,
        expected: Boolean,
    ) {
        assertThat(QuietHours.isWithin(now, start, end)).isEqualTo(expected)
    }

    @Test
    fun `non-wrapping window is inclusive of start and exclusive of end`() {
        val start = LocalTime.of(9, 0)
        val end = LocalTime.of(17, 0)

        assertThat(QuietHours.isWithin(LocalTime.of(9, 0), start, end)).isTrue()
        assertThat(QuietHours.isWithin(LocalTime.of(16, 59, 59), start, end)).isTrue()
        assertThat(QuietHours.isWithin(LocalTime.of(17, 0), start, end)).isFalse()
        assertThat(QuietHours.isWithin(LocalTime.of(8, 59, 59), start, end)).isFalse()
    }

    @Test
    fun `equal start and end is treated as an empty window`() {
        val time = LocalTime.of(10, 0)
        assertThat(QuietHours.isWithin(time, time, time)).isFalse()
    }

    companion object {
        @JvmStatic
        fun wrappingWindowCases(): Stream<Arguments> {
            val start = LocalTime.of(23, 30)
            val end = LocalTime.of(7, 0)
            return Stream.of(
                Arguments.of(LocalTime.of(23, 30), start, end, true),
                Arguments.of(LocalTime.of(23, 29, 59), start, end, false),
                Arguments.of(LocalTime.of(0, 0), start, end, true),
                Arguments.of(LocalTime.of(6, 59, 59), start, end, true),
                Arguments.of(LocalTime.of(7, 0), start, end, false),
                Arguments.of(LocalTime.of(12, 0), start, end, false),
            )
        }
    }
}
