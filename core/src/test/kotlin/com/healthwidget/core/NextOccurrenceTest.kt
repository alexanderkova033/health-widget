package com.healthwidget.core

import com.google.common.truth.Truth.assertThat
import java.time.Duration
import java.time.LocalTime
import org.junit.jupiter.api.Test

class NextOccurrenceTest {
    @Test
    fun `target later today returns the same-day delay`() {
        val now = LocalTime.of(9, 0)
        val target = LocalTime.of(14, 0)

        assertThat(durationUntilNext(target, now)).isEqualTo(Duration.ofHours(5))
    }

    @Test
    fun `target earlier today rolls over to tomorrow`() {
        val now = LocalTime.of(14, 0)
        val target = LocalTime.of(9, 0)

        assertThat(durationUntilNext(target, now)).isEqualTo(Duration.ofHours(19))
    }

    @Test
    fun `target equal to now rolls over to a full day later`() {
        val time = LocalTime.of(10, 30)

        assertThat(durationUntilNext(time, time)).isEqualTo(Duration.ofHours(24))
    }

    @Test
    fun `handles times crossing midnight`() {
        val now = LocalTime.of(23, 45)
        val target = LocalTime.of(0, 15)

        assertThat(durationUntilNext(target, now)).isEqualTo(Duration.ofMinutes(30))
    }
}
