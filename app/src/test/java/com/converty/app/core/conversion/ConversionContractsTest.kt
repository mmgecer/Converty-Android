package com.converty.app.core.conversion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConversionContractsTest {
    @Test
    fun `empty engine selection expands to every index`() {
        assertEquals(listOf(0, 1, 2, 3), normalizeSelection(emptyList(), total = 4))
    }

    @Test
    fun `engine selection is deduplicated and sorted`() {
        assertEquals(listOf(0, 2, 3), normalizeSelection(listOf(3, 0, 2, 3), total = 4))
    }

    @Test
    fun `out of bounds engine selection is rejected`() {
        assertNull(normalizeSelection(listOf(-1, 2), total = 3))
        assertNull(normalizeSelection(listOf(0, 3), total = 3))
    }

    @Test
    fun `nonpositive totals and progress fractions have deterministic boundaries`() {
        assertEquals(emptyList<Int>(), normalizeSelection(listOf(0), total = 0))
        assertEquals(0f, ConversionProgress(0, 0, ConversionProgress.Stage.READING).fraction, 0f)
        assertEquals(0.5f, ConversionProgress(2, 4, ConversionProgress.Stage.RENDERING).fraction, 0f)
    }
}
