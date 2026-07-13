package com.converty.app.feature.app

import com.converty.app.core.model.ConversionDirection
import com.converty.app.core.model.ConversionDocument
import com.converty.app.core.model.ConversionItem
import com.converty.app.core.model.ConversionJob
import com.converty.app.core.model.ConversionOutput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryOutputModelTest {
    @Test
    fun `history exposes every produced output and keeps an input fallback`() {
        val outputs = listOf(output("page-001.png"), output("page-002.png"))
        assertEquals(outputs, item(outputs).outputsForHistory())

        val fallback = item(emptyList()).outputsForHistory()
        assertEquals(1, fallback.size)
        assertNull(fallback.single())
    }

    @Test
    fun `history search matches input and every output not only the first`() {
        val job = ConversionJob(
            id = "job",
            direction = ConversionDirection.PDF_TO_PNG,
            createdAtEpochMillis = 1L,
            items = listOf(
                item(
                    outputs = listOf(
                        output("report-page-001.png"),
                        output("report-page-002.png"),
                    ),
                ),
            ),
        )

        assertTrue(job.matchesHistoryQuery("source.pdf"))
        assertTrue(job.matchesHistoryQuery("PAGE-002"))
        assertTrue(job.matchesHistoryQuery(""))
        assertFalse(job.matchesHistoryQuery("missing"))
    }

    private fun item(outputs: List<ConversionOutput>) = ConversionItem(
        id = "item",
        jobId = "job",
        position = 0,
        input = ConversionDocument(
            uri = "content://input/source.pdf",
            displayName = "source.pdf",
            mimeType = "application/pdf",
        ),
        outputs = outputs,
    )

    private fun output(name: String) = ConversionOutput(
        uri = "content://output/$name",
        displayName = name,
        mimeType = "image/png",
        createdAtEpochMillis = 2L,
    )
}
