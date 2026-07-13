package com.converty.app.core.conversion

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class PptxPackageParserTest {
    @Test
    fun `parser keeps presentation and spTree order and models basic drawing content`() {
        val file = externalStylePresentation()

        val result = PptxPackageParser.open(file)

        assertTrue(result is PptxParseResult.Success)
        (result as PptxParseResult.Success).presentation.use { presentation ->
            assertEquals(EmuSize(9_144_000, 6_858_000), presentation.slideSize)
            assertEquals(2, presentation.slides.size)

            // presentation.xml deliberately lists slide2 before slide1.
            val richSlide = presentation.slides[0]
            assertEquals(0, richSlide.index)
            assertEquals(5, richSlide.elements.size)
            assertTrue(richSlide.elements[0] is PptxSlideElement.Picture)
            assertEquals(
                listOf(
                    PptxPresetGeometry.ROUND_RECT,
                    PptxPresetGeometry.RECT,
                    PptxPresetGeometry.ELLIPSE,
                    PptxPresetGeometry.LINE,
                ),
                richSlide.elements.drop(1).map { (it as PptxSlideElement.Shape).geometry },
            )

            val picture = richSlide.elements[0] as PptxSlideElement.Picture
            assertEquals(EmuRect(0, 0, 4_572_000, 3_429_000), picture.transform.bounds)
            assertArrayEquals(IMAGE_BYTES, presentation.readImage(picture))

            val textShape = richSlide.elements[1] as PptxSlideElement.Shape
            assertEquals(PptxFill.Solid(0xFF112233.toInt()), textShape.fill)
            assertEquals(PptxFill.Solid(0xFFAABBCC.toInt()), textShape.line?.fill)
            assertEquals(25_400L, textShape.line?.widthEmu)
            assertEquals(90f, textShape.transform.rotationDegrees, 0.001f)
            assertTrue(textShape.transform.flipHorizontally)
            assertFalse(textShape.transform.flipVertically)
            val paragraph = requireNotNull(textShape.textFrame).paragraphs.single()
            assertEquals(PptxParagraphAlignment.CENTER, paragraph.alignment)
            assertEquals(listOf("Hello", "\n", "world"), paragraph.runs.map(PptxTextRun::text))
            assertEquals(24f, paragraph.runs.first().fontSizePoints, 0.001f)
            assertEquals(0xFF445566.toInt(), paragraph.runs.first().argb)
            assertTrue(paragraph.runs.first().bold)
            assertTrue(paragraph.runs.first().italic)
            assertEquals("Arial", paragraph.runs.first().fontFamily)

            assertTrue("chart-table-or-smartart" in richSlide.unsupportedFeatures)
            assertTrue("placeholder-style-fallback" in richSlide.unsupportedFeatures)
            assertTrue("theme-or-master-style-fallback" in richSlide.unsupportedFeatures)
            assertTrue("theme-or-master-text-style-fallback" in richSlide.unsupportedFeatures)
            assertTrue("theme-color-fallback:accent1" in richSlide.unsupportedFeatures)
            assertTrue(richSlide.isSupported)

            val secondSlide = presentation.slides[1]
            assertEquals(PptxPresetGeometry.ELLIPSE, (secondSlide.elements.single() as PptxSlideElement.Shape).geometry)
        }
        file.delete()
    }

    @Test
    fun `parser marks a complex-only slide as having no renderable content`() {
        val complexSlide = """
            <p:sld xmlns:p="$P"><p:cSld><p:spTree><p:nvGrpSpPr/><p:grpSpPr/><p:graphicFrame/></p:spTree></p:cSld></p:sld>
        """.trimIndent()
        val oneSlidePresentation = PRESENTATION.replace(
            "<p:sldId id=\"257\" r:id=\"rId2\"/><p:sldId id=\"256\" r:id=\"rId1\"/>",
            "<p:sldId id=\"257\" r:id=\"rId2\"/>",
        )
        val oneSlideRelationships = """
            <Relationships xmlns="$PR"><Relationship Id="rId2" Type="$R/slide" Target="slides/slide2.xml"/></Relationships>
        """.trimIndent()
        val file = zipOf(
            mapOf(
                "ppt/presentation.xml" to oneSlidePresentation.toByteArray(),
                "ppt/_rels/presentation.xml.rels" to oneSlideRelationships.toByteArray(),
                "ppt/slides/slide2.xml" to complexSlide.toByteArray(),
            ),
        )

        val result = PptxPackageParser.open(file)

        assertTrue(result is PptxParseResult.Success)
        (result as PptxParseResult.Success).presentation.use { presentation ->
            val slide = presentation.slides.single()
            assertFalse(slide.isSupported)
            assertTrue(slide.elements.isEmpty())
            assertTrue("chart-table-or-smartart" in slide.unsupportedFeatures)
            assertTrue("no-renderable-content" in slide.unsupportedFeatures)
        }
        file.delete()
    }

    private fun externalStylePresentation(): File = zipOf(
        mapOf(
            "ppt/presentation.xml" to PRESENTATION.toByteArray(),
            "ppt/_rels/presentation.xml.rels" to PRESENTATION_RELS.toByteArray(),
            "ppt/slides/slide2.xml" to RICH_SLIDE.toByteArray(),
            "ppt/slides/_rels/slide2.xml.rels" to RICH_SLIDE_RELS.toByteArray(),
            "ppt/slides/slide1.xml" to SECOND_SLIDE.toByteArray(),
            "ppt/media/image1.png" to IMAGE_BYTES,
        ),
    )

    private fun zipOf(entries: Map<String, ByteArray>): File =
        File.createTempFile("pptx-parser-test-", ".pptx").also { file ->
            ZipOutputStream(FileOutputStream(file)).use { zip ->
                entries.forEach { (name, value) ->
                    zip.putNextEntry(ZipEntry(name))
                    zip.write(value)
                    zip.closeEntry()
                }
            }
        }

    private companion object {
        val IMAGE_BYTES = byteArrayOf(9, 8, 7, 6)
        const val P = "http://schemas.openxmlformats.org/presentationml/2006/main"
        const val A = "http://schemas.openxmlformats.org/drawingml/2006/main"
        const val R = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"
        const val PR = "http://schemas.openxmlformats.org/package/2006/relationships"
        val PRESENTATION = """
            <p:presentation xmlns:p="$P" xmlns:r="$R">
              <p:sldIdLst><p:sldId id="257" r:id="rId2"/><p:sldId id="256" r:id="rId1"/></p:sldIdLst>
              <p:sldSz cx="9144000" cy="6858000"/>
            </p:presentation>
        """.trimIndent()
        val PRESENTATION_RELS = """
            <Relationships xmlns="$PR">
              <Relationship Id="rId1" Type="$R/slide" Target="slides/slide1.xml"/>
              <Relationship Id="rId2" Type="$R/slide" Target="slides/slide2.xml"/>
            </Relationships>
        """.trimIndent()
        val RICH_SLIDE_RELS = """
            <Relationships xmlns="$PR">
              <Relationship Id="rImg" Type="$R/image" Target="../media/image1.png"/>
            </Relationships>
        """.trimIndent()
        val RICH_SLIDE = """
            <p:sld xmlns:p="$P" xmlns:a="$A" xmlns:r="$R"><p:cSld><p:spTree>
              <p:nvGrpSpPr/><p:grpSpPr/>
              <p:pic><p:nvPicPr/><p:blipFill><a:blip r:embed="rImg"/><a:stretch/></p:blipFill>
                <p:spPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="4572000" cy="3429000"/></a:xfrm></p:spPr>
              </p:pic>
              <p:sp><p:nvSpPr><p:nvPr><p:ph type="body"/></p:nvPr></p:nvSpPr><p:style/>
                <p:spPr><a:xfrm rot="5400000" flipH="1"><a:off x="100000" y="200000"/><a:ext cx="3000000" cy="1000000"/></a:xfrm>
                  <a:prstGeom prst="roundRect"/><a:solidFill><a:srgbClr val="112233"/></a:solidFill>
                  <a:ln w="25400"><a:solidFill><a:srgbClr val="AABBCC"/></a:solidFill></a:ln>
                </p:spPr>
                <p:txBody><a:bodyPr lIns="12700" tIns="25400" rIns="12700" bIns="25400"/><a:p><a:pPr algn="ctr"/>
                  <a:r><a:rPr sz="2400" b="1" i="1"><a:solidFill><a:srgbClr val="445566"/></a:solidFill><a:latin typeface="Arial"/></a:rPr><a:t>Hello</a:t></a:r>
                  <a:br/><a:r><a:t>world</a:t></a:r>
                </a:p></p:txBody>
              </p:sp>
              <p:sp><p:nvSpPr/><p:spPr><a:xfrm><a:off x="0" y="4000000"/><a:ext cx="1000000" cy="500000"/></a:xfrm><a:prstGeom prst="rect"/><a:noFill/><a:ln><a:solidFill><a:srgbClr val="000000"/></a:solidFill></a:ln></p:spPr></p:sp>
              <p:sp><p:nvSpPr/><p:spPr><a:xfrm><a:off x="1500000" y="4000000"/><a:ext cx="1000000" cy="500000"/></a:xfrm><a:prstGeom prst="ellipse"/><a:solidFill><a:schemeClr val="accent1"/></a:solidFill></p:spPr></p:sp>
              <p:sp><p:nvSpPr/><p:spPr><a:xfrm><a:off x="3000000" y="4000000"/><a:ext cx="1000000" cy="0"/></a:xfrm><a:prstGeom prst="line"/><a:noFill/><a:ln><a:solidFill><a:srgbClr val="FF0000"/></a:solidFill></a:ln></p:spPr></p:sp>
              <p:graphicFrame/>
            </p:spTree></p:cSld></p:sld>
        """.trimIndent()
        val SECOND_SLIDE = """
            <p:sld xmlns:p="$P" xmlns:a="$A"><p:cSld><p:spTree><p:nvGrpSpPr/><p:grpSpPr/>
              <p:sp><p:nvSpPr/><p:spPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="1000000" cy="1000000"/></a:xfrm><a:prstGeom prst="ellipse"/><a:solidFill><a:srgbClr val="00FF00"/></a:solidFill></p:spPr></p:sp>
            </p:spTree></p:cSld></p:sld>
        """.trimIndent()
    }
}
