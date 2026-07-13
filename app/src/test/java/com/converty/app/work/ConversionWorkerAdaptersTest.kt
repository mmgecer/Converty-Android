package com.converty.app.work

import com.converty.app.core.conversion.ErrorCode
import com.converty.app.core.conversion.ImageFit
import com.converty.app.core.conversion.ImageQuality
import com.converty.app.core.model.ContentFit
import com.converty.app.core.model.ConversionDirection
import com.converty.app.core.model.ConversionDocument
import com.converty.app.core.model.ConversionErrorCode
import com.converty.app.core.model.ConversionItem
import com.converty.app.core.model.ConversionOptions
import com.converty.app.core.model.ConversionQuality
import com.converty.app.core.model.selection.SelectionErrorCode
import com.converty.app.core.model.selection.SelectionRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversionWorkerAdaptersTest {
    @Test
    fun `all selection uses engine sentinel and preserves known total`() {
        val result = resolveWorkerSelection(item(totalUnits = 7), options(SelectionRequest.all())).success()

        assertEquals(emptyList<Int>(), result.zeroBasedIndices)
        assertEquals(7, result.selectedCount)
    }

    @Test
    fun `all selection does not require an inspected total`() {
        val result = resolveWorkerSelection(item(totalUnits = null), options(SelectionRequest.all())).success()

        assertEquals(emptyList<Int>(), result.zeroBasedIndices)
        assertNull(result.selectedCount)
    }

    @Test
    fun `first selection resolves to zero based indices`() {
        assertSelection(SelectionRequest.first(2), total = 5, expected = listOf(0, 1))
    }

    @Test
    fun `last selection resolves after total is known`() {
        assertSelection(SelectionRequest.last(2), total = 5, expected = listOf(3, 4))
    }

    @Test
    fun `keep selection preserves disjoint sections`() {
        assertSelection(SelectionRequest.keep("1,3-4"), total = 5, expected = listOf(0, 2, 3))
    }

    @Test
    fun `remove selection passes the complement to the engine`() {
        assertSelection(SelectionRequest.remove("2-4"), total = 5, expected = listOf(0, 4))
    }

    @Test
    fun `non all selection fails when inspection did not provide total`() {
        val result = resolveWorkerSelection(item(totalUnits = null), options(SelectionRequest.first(1)))

        assertTrue(result is WorkerSelectionResolution.Failure)
        assertNull((result as WorkerSelectionResolution.Failure).error)
    }

    @Test
    fun `selection parser failure remains structured for the worker`() {
        val result = resolveWorkerSelection(item(totalUnits = 3), options(SelectionRequest.last(4)))

        assertTrue(result is WorkerSelectionResolution.Failure)
        assertEquals(
            SelectionErrorCode.COUNT_OUT_OF_BOUNDS,
            (result as WorkerSelectionResolution.Failure).error?.code,
        )
    }

    @Test
    fun `output name preserves Unicode`() {
        val name = buildOutputDisplayName(
            sourceDisplayName = "Çalışma 你好.PDF",
            direction = ConversionDirection.PDF_TO_PPTX,
            pattern = "{name}-dönüşüm",
        )

        assertEquals("Çalışma 你好-dönüşüm.pptx", name)
    }

    @Test
    fun `uppercase source and target extensions are not duplicated`() {
        val name = buildOutputDisplayName(
            sourceDisplayName = "DECK.PPTX",
            direction = ConversionDirection.PPTX_TO_PDF,
            pattern = "{name}.PDF",
        )

        assertEquals("DECK.PDF", name)
    }

    @Test
    fun `illegal filename characters are sanitized`() {
        val name = buildOutputDisplayName(
            sourceDisplayName = "report.pdf",
            direction = ConversionDirection.PDF_TO_PPTX,
            pattern = "{name}:final/{ext}",
        )

        assertEquals("report_final_pptx.pptx", name)
        assertFalse(Regex("[\\u0000-\\u001F\\\\/:*?\"<>|]").containsMatchIn(name))
    }

    @Test
    fun `pattern tokens trim whitespace and blank pattern falls back to source name`() {
        assertEquals(
            "report_copy.pptx",
            buildOutputDisplayName("report.pdf", ConversionDirection.PDF_TO_PPTX, "  {name}_copy.{ext}  "),
        )
        assertEquals(
            "report.pptx",
            buildOutputDisplayName("report.pdf", ConversionDirection.PDF_TO_PPTX, "   "),
        )
    }

    @Test
    fun `every engine error maps to stable domain code and retry policy`() {
        val expected = mapOf(
            ErrorCode.SOURCE_NOT_READABLE to (ConversionErrorCode.INPUT_PERMISSION_LOST to true),
            ErrorCode.DESTINATION_NOT_WRITABLE to (ConversionErrorCode.OUTPUT_PERMISSION_LOST to true),
            ErrorCode.INVALID_SELECTION to (ConversionErrorCode.INVALID_SELECTION to false),
            ErrorCode.MALFORMED_DOCUMENT to (ConversionErrorCode.INVALID_DOCUMENT to false),
            ErrorCode.ENCRYPTED_DOCUMENT to (ConversionErrorCode.PASSWORD_PROTECTED to false),
            ErrorCode.SECURITY_LIMIT_EXCEEDED to (ConversionErrorCode.UNSUPPORTED_INPUT to false),
            ErrorCode.OUT_OF_MEMORY to (ConversionErrorCode.OUT_OF_MEMORY to true),
            ErrorCode.IO_ERROR to (ConversionErrorCode.ENGINE_FAILURE to true),
            ErrorCode.INTERNAL_ERROR to (ConversionErrorCode.ENGINE_FAILURE to false),
        )

        assertEquals(ErrorCode.entries.toSet(), expected.keys)
        expected.forEach { (engineCode, expectation) ->
            val error = engineCode.toDomainError("diagnostic-$engineCode")
            assertEquals(engineCode.name, expectation.first, error.code)
            assertEquals(engineCode.name, expectation.second, error.isRetryable)
            assertEquals(engineCode.name, "diagnostic-$engineCode", error.diagnostic)
        }
    }

    @Test
    fun `worker quality mapping keeps supported engine levels`() {
        assertEquals(ImageQuality.COMPACT, ConversionQuality.COMPACT.toEngineQuality())
        assertEquals(ImageQuality.BALANCED, ConversionQuality.BALANCED.toEngineQuality())
        assertEquals(ImageQuality.HIGH, ConversionQuality.HIGH.toEngineQuality())
        assertEquals(ImageQuality.MAXIMUM, ConversionQuality.MAXIMUM.toEngineQuality())
    }

    @Test
    fun `worker fit mapping covers every domain fit`() {
        assertEquals(ImageFit.CONTAIN, ContentFit.CONTAIN.toEngineFit())
        assertEquals(ImageFit.COVER, ContentFit.COVER.toEngineFit())
        assertEquals(ImageFit.STRETCH, ContentFit.STRETCH.toEngineFit())
    }

    private fun assertSelection(request: SelectionRequest, total: Int, expected: List<Int>) {
        val result = resolveWorkerSelection(item(totalUnits = total), options(request)).success()
        assertEquals(expected, result.zeroBasedIndices)
        assertEquals(expected.size, result.selectedCount)
    }

    private fun options(selection: SelectionRequest) = ConversionOptions(selection = selection)

    private fun item(totalUnits: Int?) = ConversionItem(
        id = "item-1",
        jobId = "job-1",
        position = 0,
        input = ConversionDocument(
            uri = "content://documents/source",
            displayName = "source.pdf",
        ),
        totalUnits = totalUnits,
    )

    private fun WorkerSelectionResolution.success(): WorkerSelectionResolution.Success {
        assertTrue(this is WorkerSelectionResolution.Success)
        return this as WorkerSelectionResolution.Success
    }
}
