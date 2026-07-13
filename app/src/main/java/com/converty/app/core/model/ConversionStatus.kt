package com.converty.app.core.model

enum class ConversionStatus {
    QUEUED,
    PREPARING,
    RUNNING,
    SUCCEEDED,
    SUCCEEDED_WITH_WARNINGS,
    FAILED,
    CANCELLED,
}

val ConversionStatus.isTerminal: Boolean
    get() = this == ConversionStatus.SUCCEEDED ||
        this == ConversionStatus.SUCCEEDED_WITH_WARNINGS ||
        this == ConversionStatus.FAILED ||
        this == ConversionStatus.CANCELLED
