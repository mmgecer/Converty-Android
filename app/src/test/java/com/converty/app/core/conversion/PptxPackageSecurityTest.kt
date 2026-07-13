package com.converty.app.core.conversion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class PptxPackageSecurityTest {
    @Test
    fun `parser rejects zip path traversal`() {
        val file = zipOf(mapOf("../outside.xml" to "x"))

        val result = PptxPackageParser.open(file)

        assertTrue(result is PptxParseResult.Failure)
        assertEquals(PptxParseFailureCode.SECURITY_LIMIT, (result as PptxParseResult.Failure).code)
        file.delete()
    }

    @Test
    fun `parser rejects doctype and external entities`() {
        val presentation = """<?xml version="1.0"?>
            <!DOCTYPE p:presentation [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
            <p:presentation xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
              <p:sldSz cx="9144000" cy="6858000"/><p:extLst>&xxe;</p:extLst>
            </p:presentation>
        """.trimIndent()
        val file = zipOf(mapOf("ppt/presentation.xml" to presentation))

        val result = PptxPackageParser.open(file)

        assertTrue(result is PptxParseResult.Failure)
        assertEquals(PptxParseFailureCode.SECURITY_LIMIT, (result as PptxParseResult.Failure).code)
        file.delete()
    }

    @Test
    fun `parser enforces entry count limit before XML parsing`() {
        val file = zipOf(mapOf("one" to "1", "two" to "2"))

        val result = PptxPackageParser.open(file, PptxReadLimits(maxEntries = 1))

        assertTrue(result is PptxParseResult.Failure)
        assertEquals(PptxParseFailureCode.SECURITY_LIMIT, (result as PptxParseResult.Failure).code)
        file.delete()
    }

    private fun zipOf(entries: Map<String, String>): File =
        File.createTempFile("pptx-security-test-", ".pptx").also { file ->
            ZipOutputStream(FileOutputStream(file)).use { zip ->
                entries.forEach { (name, value) ->
                    zip.putNextEntry(ZipEntry(name))
                    zip.write(value.toByteArray())
                    zip.closeEntry()
                }
            }
        }
}
