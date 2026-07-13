package com.converty.app.core.model

/**
 * Persistable description of a Storage Access Framework document.
 *
 * [uri] is deliberately a String so the core model has no Android dependency.
 */
data class ConversionDocument(
    val uri: String,
    val displayName: String,
    val mimeType: String? = null,
    val sizeBytes: Long? = null,
    val lastModifiedEpochMillis: Long? = null,
    val hasPersistedReadPermission: Boolean = false,
) {
    init {
        require(uri.isNotBlank()) { "Document URI cannot be blank" }
        require(displayName.isNotBlank()) { "Document name cannot be blank" }
        require(sizeBytes == null || sizeBytes >= 0L) { "Document size cannot be negative" }
    }
}
