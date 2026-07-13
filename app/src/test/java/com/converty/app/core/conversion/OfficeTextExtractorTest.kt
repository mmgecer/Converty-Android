package com.converty.app.core.conversion

import com.converty.app.core.model.FileFormat
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class OfficeTextExtractorTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `docx extracts ordered paragraphs and declares basic layout warning`() {
        val document = extract(
            FileFormat.DOCX,
            "word/document.xml" to """
                <w:document xmlns:w="urn:word"><w:body>
                    <w:p><w:r><w:t>Hello </w:t></w:r><w:r><w:t>world</w:t></w:r></w:p>
                    <w:p><w:r><w:t>Second paragraph</w:t></w:r></w:p>
                </w:body></w:document>
            """.trimIndent(),
        )

        assertEquals(listOf("Hello world", "Second paragraph"), document.sections.single().lines)
        assertEquals(listOf("basic-office-layout"), document.warnings.map { it.code })
    }

    @Test
    fun `xlsx resolves shared strings numeric cells and sparse columns`() {
        val document = extract(
            FileFormat.XLSX,
            "xl/sharedStrings.xml" to """
                <sst xmlns="urn:sheet"><si><t>Name</t></si><si><t>Ada</t></si></sst>
            """.trimIndent(),
            "xl/worksheets/sheet1.xml" to """
                <worksheet xmlns="urn:sheet"><sheetData><row>
                    <c r="A1" t="s"><v>0</v></c>
                    <c r="B1" t="s"><v>1</v></c>
                    <c r="D1"><v>42</v></c>
                </row></sheetData></worksheet>
            """.trimIndent(),
        )

        assertEquals("Sheet 1", document.sections.single().title)
        assertEquals(listOf("Name    Ada        42"), document.sections.single().lines)
    }

    @Test
    fun `odt ods and odp preserve their basic section structure`() {
        val odt = extract(
            FileFormat.ODT,
            "content.xml" to """
                <office:document-content xmlns:office="urn:office" xmlns:text="urn:text">
                    <office:body><office:text><text:h>Heading</text:h><text:p>Body</text:p></office:text></office:body>
                </office:document-content>
            """.trimIndent(),
        )
        assertEquals(listOf("Heading", "Body"), odt.sections.single().lines)

        val ods = extract(
            FileFormat.ODS,
            "content.xml" to """
                <office:document-content xmlns:office="urn:office"
                    xmlns:table="urn:oasis:names:tc:opendocument:xmlns:table:1.0" xmlns:text="urn:text">
                    <office:body><table:table table:name="Budget"><table:table-row>
                        <table:table-cell><text:p>Item</text:p></table:table-cell>
                        <table:table-cell><text:p>10</text:p></table:table-cell>
                    </table:table-row></table:table></office:body>
                </office:document-content>
            """.trimIndent(),
        )
        assertEquals("Budget", ods.sections.single().title)
        assertEquals(listOf("Item    10"), ods.sections.single().lines)

        val odp = extract(
            FileFormat.ODP,
            "content.xml" to """
                <office:document-content xmlns:office="urn:office"
                    xmlns:draw="urn:oasis:names:tc:opendocument:xmlns:drawing:1.0" xmlns:text="urn:text">
                    <office:body><draw:page draw:name="Intro"><text:h>Title</text:h><text:p>Subtitle</text:p></draw:page></office:body>
                </office:document-content>
            """.trimIndent(),
        )
        assertEquals("Intro", odp.sections.single().title)
        assertEquals(listOf("Title", "Subtitle"), odp.sections.single().lines)
    }

    @Test
    fun `unsafe zip paths and doctypes are rejected before content is trusted`() {
        val unsafePath = archive("../content.xml" to "<root/>")
        assertThrows(OfficeSecurityException::class.java) {
            OfficeTextExtractor.extract(unsafePath, FileFormat.ODT)
        }

        val doctype = archive(
            "content.xml" to """
                <!DOCTYPE root [<!ENTITY secret SYSTEM "file:///etc/passwd">]>
                <root><p>&secret;</p></root>
            """.trimIndent(),
        )
        val error = assertThrows(Exception::class.java) {
            OfficeTextExtractor.extract(doctype, FileFormat.ODT)
        }
        assertTrue(error.message.orEmpty().contains("DOCTYPE", ignoreCase = true))
    }

    private fun extract(format: FileFormat, vararg entries: Pair<String, String>) =
        OfficeTextExtractor.extract(archive(*entries), format)

    private fun archive(vararg entries: Pair<String, String>): File {
        val file = temporaryFolder.newFile("office-${System.nanoTime()}.zip")
        ZipOutputStream(file.outputStream().buffered()).use { output ->
            entries.forEach { (name, content) ->
                output.putNextEntry(ZipEntry(name))
                output.write(content.toByteArray(Charsets.UTF_8))
                output.closeEntry()
            }
        }
        return file
    }
}
