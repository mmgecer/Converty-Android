package com.converty.app.core.conversion

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File

class PptxPackageWriterTest {
    @Test
    fun `writer output is deterministic`() {
        val first = buildPresentation()
        val second = buildPresentation()

        assertArrayEquals(first, second)
    }

    @Test
    fun `writer and parser round trip slide geometry and image`() {
        val bytes = buildPresentation()
        val file = temporaryFile(bytes)

        val result = PptxPackageParser.open(file)

        assertTrue(result is PptxParseResult.Success)
        (result as PptxParseResult.Success).presentation.use { presentation ->
            assertEquals(EmuSize(12_192_000, 6_858_000), presentation.slideSize)
            assertEquals(1, presentation.slides.size)
            val slide = presentation.slides.single()
            assertTrue(slide.isSupported)
            assertEquals(EmuRect(1_000, 2_000, 3_000, 4_000), slide.placement)
            assertEquals("image/png", slide.imageMimeType)
            assertArrayEquals(TEST_IMAGE, presentation.readImage(slide))
        }
        file.delete()
    }

    @Test
    fun `parser exposes exact slide count used by inspection`() {
        val bytes = ByteArrayOutputStream().use { output ->
            PptxPackageWriter(output, EmuSize(9_144_000, 6_858_000)).use { writer ->
                repeat(2) {
                    writer.addImageSlide(TEST_IMAGE, "image/png", 10, 10, EmuRect(0, 0, 9_144_000, 6_858_000))
                }
            }
            output.toByteArray()
        }
        val file = temporaryFile(bytes)

        val result = PptxPackageParser.open(file)

        assertTrue(result is PptxParseResult.Success)
        (result as PptxParseResult.Success).presentation.use { presentation ->
            assertEquals(2, presentation.slides.size)
        }
        file.delete()
    }

    @Test
    fun `contain fit preserves aspect ratio and centers image`() {
        val placement = PlacementCalculator.calculate(EmuSize(1_600, 900), 1_000, 1_000, ImageFit.CONTAIN)

        assertEquals(EmuRect(350, 0, 900, 900), placement)
    }

    private fun buildPresentation(): ByteArray = ByteArrayOutputStream().use { output ->
        PptxPackageWriter(output, EmuSize(12_192_000, 6_858_000)).use { writer ->
            writer.addImageSlide(TEST_IMAGE, "image/png", 10, 10, EmuRect(1_000, 2_000, 3_000, 4_000))
        }
        output.toByteArray()
    }

    private fun temporaryFile(bytes: ByteArray): File =
        File.createTempFile("pptx-writer-test-", ".pptx").apply { writeBytes(bytes) }

    private companion object {
        val TEST_IMAGE = byteArrayOf(1, 2, 3, 4, 5)
    }
}
