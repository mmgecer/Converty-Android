package com.converty.app.core.conversion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfRenderDimensionsTest {
    @Test
    fun `dpi scales standard PDF points to expected pixels`() {
        assertEquals(612 to 792, pdfRenderDimensions(612, 792, 72))
        assertEquals(2550 to 3300, pdfRenderDimensions(612, 792, 300))
    }

    @Test
    fun `render dimensions preserve safety edge and total pixel limits`() {
        listOf(
            Triple(10_000, 10_000, 600),
            Triple(100_000, 1, 600),
            Triple(1, 100_000, 600),
            Triple(20_000, 30_000, 300),
        ).forEach { (width, height, dpi) ->
            val (renderWidth, renderHeight) = pdfRenderDimensions(width, height, dpi)
            assertTrue(renderWidth in 1..8192)
            assertTrue(renderHeight in 1..8192)
            assertTrue(renderWidth.toLong() * renderHeight <= 64_000_000L)
        }
    }
}
