package com.converty.app.data.files

import android.content.ContentResolver
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.OutputStream

class SafInputHandle internal constructor(
    private val contentResolver: ContentResolver,
    val uri: Uri,
) {
    fun openStream(): InputStream = contentResolver.openInputStream(uri)
        ?: throw FileNotFoundException("Unable to open input URI")

    fun openFileDescriptor(): ParcelFileDescriptor = contentResolver.openFileDescriptor(uri, "r")
        ?: throw FileNotFoundException("Unable to open input file descriptor")

    fun copyTo(target: File, bufferSize: Int = DEFAULT_BUFFER_SIZE): Long {
        require(bufferSize > 0) { "Buffer size must be positive" }
        return openStream().use { input ->
            target.outputStream().use { output ->
                input.copyTo(output, bufferSize)
            }
        }
    }
}

class SafOutputHandle internal constructor(
    private val contentResolver: ContentResolver,
    val uri: Uri,
) {
    fun openStream(mode: String = "w"): OutputStream = contentResolver.openOutputStream(uri, mode)
        ?: throw FileNotFoundException("Unable to open output URI")

    fun openFileDescriptor(mode: String = "rw"): ParcelFileDescriptor =
        contentResolver.openFileDescriptor(uri, mode)
            ?: throw FileNotFoundException("Unable to open output file descriptor")

    fun delete(): Boolean = DocumentsContract.deleteDocument(contentResolver, uri)
}
