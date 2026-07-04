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
            assertThat(it.isNotBlank()).isTrue()
        }
    }

    @Test
    fun `pools contain no duplicate entries`() {
        listOf(catalog.general, catalog.morning, catalog.afternoon, catalog.evening).forEach { pool ->
            assertThat(pool.toSet()).hasSize(pool.size)
        }
    }

    @Test
    fun `sleep messages are single, non-blank, and distinct`() {
        assertThat(catalog.sleepLate).isNotEmpty()
        assertThat(catalog.sleepEarlyHours).isNotEmpty()
        assertThat(catalog.sleepLate).isNotEqualTo(catalog.sleepEarlyHours)
    }
}
