package com.converty.app.core.model

data class ConversionOutput(
    val uri: String,
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long? = null,
    val createdAtEpochMillis: Long,
    val isReadable: Boolean = true,
) {
    init {
        require(uri.isNotBlank()) { "Output URI cannot be blank" }
        require(displayName.isNotBlank()) { "Output name cannot be blank" }
        require(mimeType.isNotBlank()) { "Output MIME type cannot be blank" }
        require(sizeBytes == null || sizeBytes >= 0L) { "Output size cannot be negative" }
    }
}
