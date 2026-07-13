package com.converty.app.core.model

/** Stable error codes are mapped to localized text by the UI layer. */
enum class ConversionErrorCode {
    INPUT_PERMISSION_LOST,
    OUTPUT_PERMISSION_LOST,
    INPUT_NOT_FOUND,
    UNSUPPORTED_INPUT,
    INVALID_DOCUMENT,
    INVALID_SELECTION,
    PASSWORD_PROTECTED,
    OUT_OF_MEMORY,
    INSUFFICIENT_STORAGE,
    OUTPUT_CREATE_FAILED,
    OUTPUT_WRITE_FAILED,
    ENGINE_FAILURE,
    CANCELLED,
    UNKNOWN,
}

data class ConversionError(
    val code: ConversionErrorCode,
    /** A short diagnostic for support. Never put document contents in this field. */
    val diagnostic: String? = null,
    val isRetryable: Boolean = false,
)
