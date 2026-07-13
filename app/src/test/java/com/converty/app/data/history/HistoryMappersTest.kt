package com.converty.app.data.history

import com.converty.app.core.model.ContentFit
import com.converty.app.core.model.ConversionDirection
import com.converty.app.core.model.ConversionDocument
import com.converty.app.core.model.ConversionError
import com.converty.app.core.model.ConversionErrorCode
import com.converty.app.core.model.ConversionItem
import com.converty.app.core.model.ConversionJob
import com.converty.app.core.model.ConversionOptions
import com.converty.app.core.model.ConversionOutput
import com.converty.app.core.model.ConversionQuality
import com.converty.app.core.model.ConversionStatus
import com.converty.app.core.model.NameCollisionPolicy
import com.converty.app.core.model.OutputOptions
import com.converty.app.core.model.selection.SelectionRequest
import com.converty.app.data.history.local.ConversionItemWithOutputs
import com.converty.app.data.history.local.ConversionJobWithItems
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HistoryMappersTest {
    @Test
    fun `Room entities round trip a complete job and restore item order`() {
        val expected = job()
        val relation = ConversionJobWithItems(
            job = expected.toEntity(),
            items = expected.items.reversed().map { item ->
                ConversionItemWithOutputs(
                    item = item.toEntity(),
                    outputs = item.outputs.mapIndexed { index, output ->
                        output.toEntity(item.id, index)
                    },
                )
            },
        )

        assertEquals(expected, relation.toDomain())
    }

    @Test
    fun `warning code encoding removes blanks and duplicates`() {
        assertEquals(
            "unsupported-shape\u001Ffont-fallback",
            encodeWarningCodes(listOf("unsupported-shape", " ", "unsupported-shape", "font-fallback")),
        )
        assertNull(encodeWarningCodes(listOf("", "  ")))
    }

    private fun job(): ConversionJob {
        val output = ConversionOutput(
            uri = "content://documents/output-1",
            displayName = "deck.pdf",
            mimeType = "application/pdf",
            sizeBytes = 42_000L,
            createdAtEpochMillis = 2_000L,
            isReadable = true,
        )
        return ConversionJob(
            id = "job-1",
            direction = ConversionDirection.PPTX_TO_PDF,
            createdAtEpochMillis = 1_000L,
            startedAtEpochMillis = 1_100L,
            finishedAtEpochMillis = 2_100L,
            status = ConversionStatus.FAILED,
            options = ConversionOptions(
                selection = SelectionRequest.last(2),
                quality = ConversionQuality.HIGH,
                fit = ContentFit.STRETCH,
                output = OutputOptions(
                    destinationTreeUri = "content://tree/output",
                    fileNamePattern = "{name}-converted.{ext}",
                    collisionPolicy = NameCollisionPolicy.REPLACE,
                ),
            ),
            items = listOf(
                ConversionItem(
                    id = "item-1",
                    jobId = "job-1",
                    position = 0,
                    input = ConversionDocument(
                        uri = "content://documents/input-1",
                        displayName = "deck.pptx",
                        mimeType = "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                        sizeBytes = 12_000L,
                        lastModifiedEpochMillis = 900L,
                        hasPersistedReadPermission = true,
                    ),
                    outputs = listOf(output),
                    totalUnits = 4,
                    selectedUnits = 2,
                    progressPercent = 100,
                    status = ConversionStatus.SUCCEEDED_WITH_WARNINGS,
                    warningCodes = listOf("unsupported-shape", "font-fallback"),
                ),
                ConversionItem(
                    id = "item-2",
                    jobId = "job-1",
                    position = 1,
                    input = ConversionDocument(
                        uri = "content://documents/input-2",
                        displayName = "broken.pptx",
                    ),
                    progressPercent = 35,
                    status = ConversionStatus.FAILED,
                    error = ConversionError(
                        code = ConversionErrorCode.INVALID_DOCUMENT,
                        diagnostic = "malformed fixture",
                        isRetryable = false,
                    ),
                ),
            ),
            error = ConversionError(
                code = ConversionErrorCode.ENGINE_FAILURE,
                diagnostic = "one or more items failed",
                isRetryable = true,
            ),
        )
    }
}
