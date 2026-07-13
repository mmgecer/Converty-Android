package com.converty.app.data.files

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import com.converty.app.core.model.ConversionDocument

data class SafDocumentMetadata(
    val uri: Uri,
    val displayName: String,
    val mimeType: String?,
    val sizeBytes: Long?,
    val lastModifiedEpochMillis: Long?,
    val hasPersistedReadPermission: Boolean,
) {
    fun toCoreModel() = ConversionDocument(
        uri = uri.toString(),
        displayName = displayName,
        mimeType = mimeType,
        sizeBytes = sizeBytes,
        lastModifiedEpochMillis = lastModifiedEpochMillis,
        hasPersistedReadPermission = hasPersistedReadPermission,
    )
}

class SafDocumentMetadataReader(
    private val contentResolver: ContentResolver,
) {
    fun read(uri: Uri): SafDocumentMetadata {
        var displayName: String? = null
        var sizeBytes: Long? = null
        var lastModified: Long? = null
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                displayName = cursor.stringOrNull(OpenableColumns.DISPLAY_NAME)
                sizeBytes = cursor.longOrNull(OpenableColumns.SIZE)
                lastModified = cursor.longOrNull(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
            }
        }
        return SafDocumentMetadata(
            uri = uri,
            displayName = displayName?.takeIf(String::isNotBlank)
                ?: uri.lastPathSegment
                ?: "document",
            mimeType = contentResolver.getType(uri),
            sizeBytes = sizeBytes?.takeIf { it >= 0L },
            lastModifiedEpochMillis = lastModified?.takeIf { it > 0L },
            hasPersistedReadPermission = contentResolver.persistedUriPermissions.any { permission ->
                permission.uri == uri && permission.isReadPermission
            },
        )
    }
}

private fun android.database.Cursor.stringOrNull(columnName: String): String? {
    val index = getColumnIndex(columnName)
    return if (index < 0 || isNull(index)) null else getString(index)
}

private fun android.database.Cursor.longOrNull(columnName: String): Long? {
    val index = getColumnIndex(columnName)
    return if (index < 0 || isNull(index)) null else getLong(index)
}
