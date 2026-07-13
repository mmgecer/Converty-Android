package com.converty.app.core.conversion

import android.graphics.Bitmap
import android.graphics.Color
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.zip.InflaterInputStream

internal data class DecodedRaster(
    val bitmap: Bitmap,
    val warnings: List<ConversionWarning> = emptyList(),
)

internal object RasterDecoders {
    private const val MAX_PIXELS = 64_000_000L

    fun decodeBmp(file: File): DecodedRaster {
        val bytes = file.readBytes()
        requireRange(bytes, 0, 54)
        if (bytes[0].toInt() != 'B'.code || bytes[1].toInt() != 'M'.code) {
            throw IOException("Invalid BMP signature")
        }
        val pixelOffset = bytes.leInt(10)
        val dibSize = bytes.leInt(14)
        if (dibSize < 40) throw IOException("Unsupported BMP header")
        val width = bytes.leInt(18)
        val signedHeight = bytes.leInt(22)
        val height = kotlin.math.abs(signedHeight)
        val planes = bytes.leShort(26)
        val bitsPerPixel = bytes.leShort(28)
        val compression = bytes.leInt(30)
        if (width <= 0 || height <= 0 || width.toLong() * height > MAX_PIXELS) {
            throw IOException("BMP dimensions exceed the safe processing limit")
        }
        if (planes != 1 || bitsPerPixel !in setOf(24, 32) || compression != 0) {
            throw IOException("Only uncompressed 24-bit and 32-bit BMP files are supported")
        }
        val bytesPerPixel = bitsPerPixel / 8
        val rowStride = ((width * bytesPerPixel + 3) / 4) * 4
        requireRange(bytes, pixelOffset, rowStride * height)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val row = IntArray(width)
        for (outputY in 0 until height) {
            val sourceY = if (signedHeight > 0) height - 1 - outputY else outputY
            var offset = pixelOffset + sourceY * rowStride
            for (x in 0 until width) {
                val blue = bytes[offset].u8()
                val green = bytes[offset + 1].u8()
                val red = bytes[offset + 2].u8()
                val alpha = if (bytesPerPixel == 4) bytes[offset + 3].u8() else 255
                row[x] = Color.argb(alpha, red, green, blue)
                offset += bytesPerPixel
            }
            bitmap.setPixels(row, 0, width, 0, outputY, width, 1)
        }
        return DecodedRaster(bitmap)
    }

    fun decodeTiff(file: File): DecodedRaster {
        val bytes = file.readBytes()
        if (bytes.size < 8) throw IOException("TIFF file is truncated")
        val littleEndian = when (String(bytes, 0, 2, Charsets.US_ASCII)) {
            "II" -> true
            "MM" -> false
            else -> throw IOException("Invalid TIFF byte order")
        }
        val reader = TiffReader(bytes, littleEndian)
        if (reader.u16(2) != 42) throw IOException("Invalid TIFF signature")
        val firstIfd = reader.u32(4).toInt()
        val fields = reader.readIfd(firstIfd)
        val width = fields.singleInt(reader, 256)
        val height = fields.singleInt(reader, 257)
        if (width <= 0 || height <= 0 || width.toLong() * height > MAX_PIXELS) {
            throw IOException("TIFF dimensions exceed the safe processing limit")
        }
        val compression = fields.singleInt(reader, 259, 1)
        val photometric = fields.singleInt(reader, 262, 2)
        val samplesPerPixel = fields.singleInt(reader, 277, if (photometric == 2) 3 else 1)
        val rowsPerStrip = fields.singleInt(reader, 278, height).coerceAtLeast(1)
        val planar = fields.singleInt(reader, 284, 1)
        val predictor = fields.singleInt(reader, 317, 1)
        val bits = fields.values(reader, 258).ifEmpty { List(samplesPerPixel) { 8L } }
        val validSamples = when (photometric) {
            2 -> samplesPerPixel in 3..4
            else -> samplesPerPixel in 1..2
        }
        if (planar != 1 || !validSamples || bits.size != samplesPerPixel || bits.any { it != 8L }) {
            throw IOException("Only chunky 8-bit TIFF samples are supported")
        }
        if (photometric !in 0..2) {
            throw IOException("Palette and separated-color TIFF files are not supported")
        }
        val stripOffsets = fields.values(reader, 273)
        val stripByteCounts = fields.values(reader, 279)
        if (stripOffsets.isEmpty() || stripOffsets.size != stripByteCounts.size) {
            throw IOException("TIFF strip table is missing or malformed")
        }
        val expectedStripCount = (
            (height.toLong() + rowsPerStrip.toLong() - 1L) / rowsPerStrip.toLong()
        ).toInt()
        if (stripOffsets.size != expectedStripCount) {
            throw IOException("TIFF strip count does not cover the image")
        }
        val rowBytes = Math.multiplyExact(width, samplesPerPixel)
        val raster = ByteArray(Math.multiplyExact(rowBytes, height))
        stripOffsets.indices.forEach { stripIndex ->
            val startRow = stripIndex * rowsPerStrip
            if (startRow >= height) return@forEach
            val rows = minOf(rowsPerStrip, height - startRow)
            val expected = Math.multiplyExact(rowBytes, rows)
            val offset = stripOffsets[stripIndex].toInt()
            val count = stripByteCounts[stripIndex].toInt()
            requireRange(bytes, offset, count)
            val encoded = bytes.copyOfRange(offset, offset + count)
            val decoded = when (compression) {
                1 -> encoded
                5 -> decodeTiffLzw(encoded, expected)
                8, 32946 -> inflateTiffStrip(encoded, expected)
                32773 -> decodePackBits(encoded, expected)
                else -> throw IOException("Unsupported TIFF compression: $compression")
            }
            if (decoded.size < expected) throw IOException("TIFF strip is truncated")
            val strip = decoded.copyOf(expected)
            if (predictor == 2) applyHorizontalPredictor(strip, rowBytes, samplesPerPixel)
            else if (predictor != 1) throw IOException("Unsupported TIFF predictor: $predictor")
            strip.copyInto(raster, startRow * rowBytes)
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val row = IntArray(width)
        for (y in 0 until height) {
            var offset = y * rowBytes
            for (x in 0 until width) {
                row[x] = when (photometric) {
                    2 -> {
                        val red = raster[offset].u8()
                        val green = raster[offset + 1].u8()
                        val blue = raster[offset + 2].u8()
                        val alpha = if (samplesPerPixel >= 4) raster[offset + 3].u8() else 255
                        Color.argb(alpha, red, green, blue)
                    }
                    else -> {
                        var gray = raster[offset].u8()
                        if (photometric == 0) gray = 255 - gray
                        val alpha = if (samplesPerPixel >= 2) raster[offset + 1].u8() else 255
                        Color.argb(alpha, gray, gray, gray)
                    }
                }
                offset += samplesPerPixel
            }
            bitmap.setPixels(row, 0, width, 0, y, width, 1)
        }
        val nextIfdOffsetPosition = firstIfd + 2 + reader.u16(firstIfd) * 12
        val hasMorePages = runCatching { reader.u32(nextIfdOffsetPosition) != 0L }.getOrDefault(false)
        return DecodedRaster(
            bitmap,
            warnings = if (hasMorePages) {
                listOf(
                    ConversionWarning(
                        "tiff-first-page-only",
                        "Only the first TIFF page was converted",
                    ),
                )
            } else {
                emptyList()
            },
        )
    }

    private fun applyHorizontalPredictor(data: ByteArray, rowBytes: Int, samples: Int) {
        var rowStart = 0
        while (rowStart < data.size) {
            for (index in rowStart + samples until minOf(rowStart + rowBytes, data.size)) {
                data[index] = (data[index].u8() + data[index - samples].u8()).toByte()
            }
            rowStart += rowBytes
        }
    }

    private fun decodePackBits(input: ByteArray, expected: Int): ByteArray {
        val output = ArrayList<Byte>(expected)
        var index = 0
        while (index < input.size && output.size < expected) {
            val header = input[index++].toInt()
            when {
                header in 0..127 -> {
                    val count = header + 1
                    requireRange(input, index, count)
                    repeat(count) { output += input[index++] }
                }
                header in -127..-1 -> {
                    requireRange(input, index, 1)
                    val value = input[index++]
                    repeat(1 - header) { output += value }
                }
                else -> Unit
            }
            if (output.size > expected) throw IOException("TIFF PackBits data exceeds strip size")
        }
        return output.toByteArray()
    }

    private fun decodeTiffLzw(input: ByteArray, expected: Int): ByteArray {
        val dictionary = arrayOfNulls<ByteArray>(4096)
        fun reset() {
            dictionary.fill(null)
            for (index in 0..255) dictionary[index] = byteArrayOf(index.toByte())
        }
        reset()
        var bitPosition = 0
        var codeSize = 9
        var nextCode = 258
        var previous: ByteArray? = null
        val output = ArrayList<Byte>(expected)

        fun readCode(): Int? {
            if (bitPosition + codeSize > input.size * 8) return null
            var code = 0
            repeat(codeSize) {
                val byte = input[bitPosition / 8].u8()
                val bit = (byte shr (7 - bitPosition % 8)) and 1
                code = (code shl 1) or bit
                bitPosition++
            }
            return code
        }

        while (output.size < expected) {
            val code = readCode() ?: break
            when (code) {
                256 -> {
                    reset()
                    codeSize = 9
                    nextCode = 258
                    previous = null
                }
                257 -> break
                else -> {
                    val entry = when {
                        code < nextCode -> dictionary[code]
                        code == nextCode && previous != null -> previous + previous[0]
                        else -> null
                    } ?: throw IOException("Malformed TIFF LZW stream")
                    entry.forEach { output += it }
                    if (output.size > expected) throw IOException("TIFF LZW data exceeds strip size")
                    previous?.let { prior ->
                        if (nextCode < dictionary.size) {
                            dictionary[nextCode++] = prior + entry[0]
                            if (codeSize < 12 && nextCode == (1 shl codeSize) - 1) codeSize++
                        }
                    }
                    previous = entry
                }
            }
        }
        return output.toByteArray()
    }

    private data class TiffField(val type: Int, val count: Int, val valuePosition: Int)

    private class TiffReader(private val bytes: ByteArray, private val little: Boolean) {
        fun u16(offset: Int): Int {
            requireRange(bytes, offset, 2)
            val a = bytes[offset].u8()
            val b = bytes[offset + 1].u8()
            return if (little) a or (b shl 8) else (a shl 8) or b
        }

        fun u32(offset: Int): Long {
            requireRange(bytes, offset, 4)
            var result = 0L
            if (little) {
                for (index in 3 downTo 0) result = (result shl 8) or bytes[offset + index].u8().toLong()
            } else {
                for (index in 0..3) result = (result shl 8) or bytes[offset + index].u8().toLong()
            }
            return result
        }

        fun readIfd(offset: Int): Map<Int, TiffField> {
            val count = u16(offset)
            if (count > 512) throw IOException("TIFF has too many fields")
            val fields = mutableMapOf<Int, TiffField>()
            repeat(count) { index ->
                val entry = offset + 2 + index * 12
                val tag = u16(entry)
                val type = u16(entry + 2)
                val valueCount = u32(entry + 4).toInt()
                val typeSize = when (type) {
                    1, 2, 6, 7 -> 1
                    3, 8 -> 2
                    4, 9, 11 -> 4
                    5, 10, 12 -> 8
                    else -> 1
                }
                val bytesNeeded = Math.multiplyExact(valueCount, typeSize)
                val position = if (bytesNeeded <= 4) entry + 8 else u32(entry + 8).toInt()
                requireRange(bytes, position, bytesNeeded)
                fields[tag] = TiffField(type, valueCount, position)
            }
            return fields
        }

        fun values(field: TiffField): List<Long> = when (field.type) {
            1 -> List(field.count) { bytes[field.valuePosition + it].u8().toLong() }
            3 -> List(field.count) { u16(field.valuePosition + it * 2).toLong() }
            4 -> List(field.count) { u32(field.valuePosition + it * 4) }
            else -> throw IOException("Unsupported TIFF field type: ${field.type}")
        }
    }

    private fun Map<Int, TiffField>.values(reader: TiffReader, tag: Int): List<Long> =
        get(tag)?.let(reader::values).orEmpty()

    private fun Map<Int, TiffField>.singleInt(
        reader: TiffReader,
        tag: Int,
        default: Int? = null,
    ): Int = values(reader, tag).firstOrNull()?.toInt()
        ?: default
        ?: throw IOException("Required TIFF tag $tag is missing")

    private fun Byte.u8(): Int = toInt() and 0xFF
    private fun ByteArray.leShort(offset: Int): Int {
        requireRange(this, offset, 2)
        return this[offset].u8() or (this[offset + 1].u8() shl 8)
    }
    private fun ByteArray.leInt(offset: Int): Int {
        requireRange(this, offset, 4)
        return this[offset].u8() or
            (this[offset + 1].u8() shl 8) or
            (this[offset + 2].u8() shl 16) or
            (this[offset + 3].u8() shl 24)
    }

    private fun requireRange(bytes: ByteArray, offset: Int, length: Int) {
        if (offset < 0 || length < 0 || offset.toLong() + length > bytes.size) {
            throw IOException("Raster file is truncated")
        }
    }
}

internal fun inflateTiffStrip(input: ByteArray, expected: Int): ByteArray {
    if (expected < 0) throw IOException("TIFF strip size is invalid")
    val output = ByteArrayOutputStream(minOf(expected, 32 * 1024))
    InflaterInputStream(ByteArrayInputStream(input)).use { stream ->
        val buffer = ByteArray(16 * 1024)
        var total = 0
        while (true) {
            val read = stream.read(buffer)
            if (read < 0) break
            if (total + read > expected) {
                throw IOException("TIFF Deflate data exceeds strip size")
            }
            output.write(buffer, 0, read)
            total += read
        }
    }
    return output.toByteArray()
}
