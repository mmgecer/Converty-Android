package com.converty.app.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ConversionModelInvariantsTest {
    @Test
    fun `item accepts inclusive progress and nonnegative unit boundaries`() {
        val item = item(totalUnits = 0, selectedUnits = 0, progressPercent = 100)

        assertEquals(0, item.totalUnits)
        assertEquals(0, item.selectedUnits)
        assertEquals(100, item.progressPercent)
    }

    @Test
    fun `item rejects invalid identity position units and progress`() {
        val invalidItems = listOf<() -> ConversionItem>(
            { item(id = " ") },
            { item(jobId = "") },
            { item(position = -1) },
            { item(totalUnits = -1) },
            { item(selectedUnits = -1) },
            { item(progressPercent = -1) },
            { item(progressPercent = 101) },
        )

        invalidItems.forEach { create ->
            assertThrows(IllegalArgumentException::class.java) { create() }
        }
    }

    @Test
    fun `job accepts items with matching id and distinct positions`() {
        val job = ConversionJob(
            id = "job-1",
            direction = ConversionDirection.PDF_TO_PPTX,
            createdAtEpochMillis = 10L,
            items = listOf(item(id = "item-1", position = 0), item(id = "item-2", position = 1)),
        )

        assertEquals(listOf(0, 1), job.items.map(ConversionItem::position))
    }

    @Test
    fun `job rejects blank id`() {
        assertThrows(IllegalArgumentException::class.java) {
            ConversionJob(
                id = " ",
                direction = ConversionDirection.PDF_TO_PPTX,
                createdAtEpochMillis = 10L,
            )
        }
    }

    @Test
    fun `job rejects item owned by another job`() {
        assertThrows(IllegalArgumentException::class.java) {
            ConversionJob(
                id = "job-1",
                direction = ConversionDirection.PDF_TO_PPTX,
                createdAtEpochMillis = 10L,
                items = listOf(item(jobId = "job-2")),
            )
        }
    }

    @Test
    fun `job rejects duplicate item positions`() {
        assertThrows(IllegalArgumentException::class.java) {
            ConversionJob(
                id = "job-1",
                direction = ConversionDirection.PDF_TO_PPTX,
                createdAtEpochMillis = 10L,
                items = listOf(item(id = "item-1", position = 2), item(id = "item-2", position = 2)),
            )
        }
    }

    private fun item(
        id: String = "item-1",
        jobId: String = "job-1",
        position: Int = 0,
        totalUnits: Int? = null,
        selectedUnits: Int? = null,
        progressPercent: Int = 0,
    ) = ConversionItem(
        id = id,
        jobId = jobId,
        position = position,
        input = ConversionDocument("content://documents/source", "source.pdf"),
        totalUnits = totalUnits,
        selectedUnits = selectedUnits,
        progressPercent = progressPercent,
    )
}
