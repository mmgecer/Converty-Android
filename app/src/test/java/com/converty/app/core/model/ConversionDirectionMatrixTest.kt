package com.converty.app.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversionDirectionMatrixTest {
    @Test
    fun `matrix is exactly the requested set and never contains same format pairs`() {
        val expected = setOf(
            FileFormat.PDF to FileFormat.PPTX,
            FileFormat.PPTX to FileFormat.PDF,
            FileFormat.PNG to FileFormat.JPG,
            FileFormat.JPG to FileFormat.PNG,
            FileFormat.PNG to FileFormat.WEBP,
            FileFormat.WEBP to FileFormat.PNG,
            FileFormat.JPG to FileFormat.WEBP,
            FileFormat.WEBP to FileFormat.JPG,
            FileFormat.TIFF to FileFormat.PNG,
            FileFormat.TIFF to FileFormat.JPG,
            FileFormat.BMP to FileFormat.PNG,
            FileFormat.BMP to FileFormat.JPG,
            FileFormat.HEIC_HEIF to FileFormat.JPG,
            FileFormat.HEIC_HEIF to FileFormat.PNG,
            FileFormat.SVG to FileFormat.PNG,
            FileFormat.AVIF to FileFormat.PNG,
            FileFormat.AVIF to FileFormat.JPG,
            FileFormat.AVIF to FileFormat.WEBP,
            FileFormat.DOCX to FileFormat.PDF,
            FileFormat.XLSX to FileFormat.PDF,
            FileFormat.ODT to FileFormat.PDF,
            FileFormat.ODS to FileFormat.PDF,
            FileFormat.ODP to FileFormat.PDF,
            FileFormat.PDF to FileFormat.PNG,
            FileFormat.PDF to FileFormat.JPG,
            FileFormat.PDF to FileFormat.TIFF,
            FileFormat.PDF to FileFormat.THUMBNAIL,
        )
        val actual = ConversionDirection.entries.map { it.sourceFormat to it.targetFormat }.toSet()

        assertEquals(expected, actual)
        assertEquals(expected.size, ConversionDirection.entries.size)
        assertTrue(ConversionDirection.entries.all { it.sourceFormat != it.targetFormat })
        assertTrue(ConversionDirection.entries.all { it.sourceFormat.canBeSource })
    }

    @Test
    fun `source target lookup is unique and swap only exists for registered reverse pairs`() {
        ConversionDirection.entries.forEach { direction ->
            assertSame(
                direction,
                ConversionDirection.from(direction.sourceFormat, direction.targetFormat),
            )
            assertEquals(
                ConversionDirection.from(direction.targetFormat, direction.sourceFormat),
                direction.reversedOrNull(),
            )
        }
        ConversionDirection.sourceFormats.forEach { source ->
            val targets = ConversionDirection.targetsFor(source)
            assertEquals(targets.distinct(), targets)
            assertFalse(source in targets)
        }

        assertSame(
            ConversionDirection.JPG_TO_PNG,
            ConversionDirection.PNG_TO_JPG.reversedOrNull(),
        )
        assertNull(ConversionDirection.TIFF_TO_PNG.reversedOrNull())
        assertNull(ConversionDirection.PDF_TO_THUMBNAIL.reversedOrNull())
    }

    @Test
    fun `format aliases mime matching and option capabilities are explicit`() {
        assertTrue(FileFormat.JPG.accepts("PHOTO.JPEG", null))
        assertTrue(FileFormat.HEIC_HEIF.accepts("capture.HEIF", null))
        assertTrue(FileFormat.HEIC_HEIF.accepts("capture.bin", "IMAGE/HEIC"))
        assertFalse(FileFormat.THUMBNAIL.canBeSource)

        assertTrue(ConversionDirection.PDF_TO_PPTX.preservesAppearanceAsImages)
        assertTrue(ConversionDirection.PDF_TO_PPTX.supportsDpi)
        assertTrue(ConversionDirection.PDF_TO_PNG.supportsItemSelection)
        assertFalse(ConversionDirection.PDF_TO_THUMBNAIL.supportsItemSelection)
        assertFalse(ConversionDirection.PDF_TO_THUMBNAIL.supportsDpi)
        assertTrue(ConversionDirection.PNG_TO_WEBP.supportsLosslessChoice)
        assertTrue(ConversionDirection.PNG_TO_JPG.alwaysLossless.not())
        assertTrue(ConversionDirection.PDF_TO_TIFF.alwaysLossless)
    }
}
