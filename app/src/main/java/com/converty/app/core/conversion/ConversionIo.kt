package com.converty.app.core.conversion

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

internal class ConversionCancelledException : IOException("Conversion cancelled")
internal class SourceLimitExceededException(message: String) : IOException(message)
internal class SourceNotReadableException(message: String, cause: Throwable? = null) : IOException(message, cause)
internal class DestinationNotWritableException(message: String, cause: Throwable? = null) : IOException(message, cause)

internal fun Context.copyUriToTemporaryFile(
    uri: Uri,
    suffix: String,
    cancellation: ConversionCancellation,
    maxBytes: Long,
): File {
    val target = File.createTempFile("converty-source-", suffix, cacheDir)
    try {
        val source = try {
            contentResolver.openInputStream(uri)
                ?: throw SourceNotReadableException("Source URI cannot be opened")
        } catch (error: SecurityException) {
            throw SourceNotReadableException("Permission to read source URI was denied", error)
        } catch (error: SourceNotReadableException) {
            throw error
        } catch (error: IOException) {
            throw SourceNotReadableException("Source URI cannot be opened", error)
        }
        source.use { input ->
            FileOutputStream(target).use { output ->
                val buffer = ByteArray(64 * 1024)
                var total = 0L
                while (true) {
                    if (cancellation.isCancellationRequested()) throw ConversionCancelledException()
                    val read = input.read(buffer)
                    if (read < 0) break
                    total += read
                    if (total > maxBytes) throw SourceLimitExceededException("Source is larger than the safe processing limit")
                    output.write(buffer, 0, read)
                }
            }
        }
        return target
    } catch (error: Throwable) {
        target.delete()
        throw error
    }
}

internal fun Context.copyTemporaryFileToUri(
    source: File,
    destination: Uri,
    cancellation: ConversionCancellation,
): Long {
    val output = try {
        contentResolver.openOutputStream(destination, "wt")
            ?: throw DestinationNotWritableException("Destination URI cannot be opened")
    } catch (error: SecurityException) {
        throw DestinationNotWritableException("Permission to write destination URI was denied", error)
    } catch (error: DestinationNotWritableException) {
        throw error
    } catch (error: IOException) {
        throw DestinationNotWritableException("Destination URI cannot be opened", error)
    }
    var total = 0L
    output.use { destinationStream ->
        source.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                if (cancellation.isCancellationRequested()) throw ConversionCancelledException()
                val read = input.read(buffer)
                if (read < 0) break
                destinationStream.write(buffer, 0, read)
                total += read
            }
            destinationStream.flush()
        }
    }
    return total
}
