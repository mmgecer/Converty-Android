package com.converty.app.data.files

import android.content.Context
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import java.io.FileNotFoundException
import java.io.IOException

/** All Android URI operations used by conversion engines live behind this gateway. */
class SafDocumentGateway(
    private val context: Context,
    private val contentResolver: ContentResolver = context.contentResolver,
    private val metadataReader: SafDocumentMetadataReader = SafDocumentMetadataReader(contentResolver),
) {
    fun metadata(uri: Uri): SafDocumentMetadata = metadataReader.read(uri)

    fun input(uri: Uri): SafInputHandle = SafInputHandle(contentResolver, uri)

    fun output(uri: Uri): SafOutputHandle = SafOutputHandle(contentResolver, uri)

    fun createOutput(treeUri: Uri, mimeType: String, displayName: String): SafOutputHandle {
        require(mimeType.isNotBlank()) { "MIME type cannot be blank" }
        require(displayName.isNotBlank()) { "Display name cannot be blank" }
        val directoryUri = DocumentsContract.buildDocumentUriUsingTree(
            treeUri,
            DocumentsContract.getTreeDocumentId(treeUri),
        )
        val outputUri = DocumentsContract.createDocument(
            contentResolver,
            directoryUri,
            mimeType,
            displayName,
        ) ?: throw FileNotFoundException("Provider could not create output document")
        return output(outputUri)
    }

    /** Returns an existing direct child without assuming a filesystem path. */
    fun findOutput(treeUri: Uri, displayName: String): SafOutputHandle? {
        require(displayName.isNotBlank()) { "Display name cannot be blank" }
        val tree = DocumentFile.fromTreeUri(context, treeUri)
            ?: throw FileNotFoundException("Output tree is not available")
        val child = tree.listFiles().firstOrNull { document ->
            document.isFile && document.name?.equals(displayName, ignoreCase = true) == true
        } ?: return null
        return output(child.uri)
    }

    fun takePersistableReadPermission(uri: Uri) {
        contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    fun takePersistableTreePermission(treeUri: Uri, includeWrite: Boolean = true) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
            (if (includeWrite) Intent.FLAG_GRANT_WRITE_URI_PERMISSION else 0)
        contentResolver.takePersistableUriPermission(treeUri, flags)
    }

    fun releasePersistablePermission(uri: Uri) {
        contentResolver.persistedUriPermissions
            .firstOrNull { it.uri == uri }
            ?.let { permission ->
                var flags = 0
                if (permission.isReadPermission) flags = flags or Intent.FLAG_GRANT_READ_URI_PERMISSION
                if (permission.isWritePermission) flags = flags or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                if (flags != 0) contentResolver.releasePersistableUriPermission(uri, flags)
            }
    }

}

class OutputAlreadyExistsException(displayName: String) :
    IOException("An output named '$displayName' already exists")
