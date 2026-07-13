package com.converty.app.core.conversion

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.DeflaterOutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class RasterDecodersSecurityTest {
    @Test
    fun `deflate strip is decoded only up to its declared raster size`() {
        val raster = ByteArray(64 * 1024) { (it and 0xFF).toByte() }
        val compressed = ByteArrayOutputStream().use { bytes ->
            DeflaterOutputStream(bytes).use { it.write(raster) }
            bytes.toByteArray()
        }

        assertArrayEquals(raster, inflateTiffStrip(compressed, raster.size))
        assertThrows(IOException::class.java) {
            inflateTiffStrip(compressed, raster.size - 1)
        }
    }

    @Test
    fun `negative declared strip size is rejected`() {
        assertThrows(IOException::class.java) { inflateTiffStrip(byteArrayOf(), -1) }
    }
}
