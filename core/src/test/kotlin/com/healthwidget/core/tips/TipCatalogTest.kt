package com.healthwidget.core.tips

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class TipCatalogTest {
    private val catalog = TipCatalog.loadDefault()

    @Test
    fun `all pools are non-empty`() {
        assertThat(catalog.general).isNotEmpty()
        assertThat(catalog.morning).isNotEmpty()
        assertThat(catalog.afternoon).isNotEmpty()
        assertThat(catalog.evening).isNotEmpty()
    }

    @Test
    fun `pools contain no blank entries`() {
        (catalog.general + catalog.morning + catalog.afternoon + catalog.evening).forEach {
            assertThat(it.text.isNotBlank()).isTrue()
        }
    }

    @Test
    fun `pools contain no duplicate entries`() {
        listOf(catalog.general, catalog.morning, catalog.afternoon, catalog.evening).forEach { pool ->
            assertThat(pool.map { it.text }.toSet()).hasSize(pool.size)
        }
    }

    @Test
    fun `sleep messages are single, non-blank, and distinct`() {
        assertThat(catalog.sleepLate.text.isNotBlank()).isTrue()
        assertThat(catalog.sleepEarlyHours.text.isNotBlank()).isTrue()
        assertThat(catalog.sleepLate.text).isNotEqualTo(catalog.sleepEarlyHours.text)
    }

    @Test
    fun `every tip has a real citation`() {
        val allTips =
            catalog.general + catalog.morning + catalog.afternoon + catalog.evening +
                listOf(catalog.sleepLate, catalog.sleepEarlyHours)
        allTips.forEach { tip ->
            assertThat(tip.sourceLabel.isNotBlank()).isTrue()
            assertThat(tip.sourceUrl.isNotBlank()).isTrue()
        }
    }
}
