package com.converty.app.work

import com.converty.app.core.conversion.ErrorCode
import com.converty.app.core.conversion.ImageFit
import com.converty.app.core.conversion.ImageQuality
import com.converty.app.core.model.ContentFit
import com.converty.app.core.model.ConversionDirection
import com.converty.app.core.model.ConversionError
import com.converty.app.core.model.ConversionErrorCode
import com.converty.app.core.model.ConversionItem
import com.converty.app.core.model.ConversionOptions
import com.converty.app.core.model.ConversionQuality
import com.converty.app.core.model.selection.SelectionError
import com.converty.app.core.model.selection.SelectionMode
import com.converty.app.core.model.selection.SelectionParseResult
import com.converty.app.core.model.selection.SelectionParser

internal sealed interface WorkerSelectionResolution {
    data class Success(
        val zeroBasedIndices: List<Int>,
        val selectedCount: Int?,
    ) : WorkerSelectionResolution

    data class Failure(val error: SelectionError?) : WorkerSelectionResolution
}

internal fun resolveWorkerSelection(
    item: ConversionItem,
    options: ConversionOptions,
): WorkerSelectionResolution {
    if (options.selection.mode == SelectionMode.ALL) {
        // ConversionEngine deliberately uses an empty list as its efficient "all" sentinel.
        return WorkerSelectionResolution.Success(emptyList(), item.totalUnits)
    }
    val total = item.totalUnits ?: return WorkerSelectionResolution.Failure(null)
    return when (val result = SelectionParser.resolve(options.selection, total)) {
        is SelectionParseResult.Failure -> WorkerSelectionResolution.Failure(result.error)
        is SelectionParseResult.Success -> WorkerSelectionResolution.Success(
            zeroBasedIndices = result.selection.indices().toList(),
            selectedCount = result.selection.selectedCount,
        )
    }
}

internal fun ConversionQuality.toEngineQuality(): ImageQuality = when (this) {
    ConversionQuality.COMPACT -> ImageQuality.COMPACT
    ConversionQuality.BALANCED -> ImageQuality.BALANCED
    ConversionQuality.HIGH -> ImageQuality.HIGH
    ConversionQuality.MAXIMUM -> ImageQuality.MAXIMUM
}

internal fun ContentFit.toEngineFit(): ImageFit = when (this) {
    ContentFit.CONTAIN -> ImageFit.CONTAIN
    ContentFit.COVER -> ImageFit.COVER
    ContentFit.STRETCH -> ImageFit.STRETCH
}

internal fun ErrorCode.toDomainError(message: String): ConversionError = when (this) {
    ErrorCode.SOURCE_NOT_READABLE -> ConversionError(
        ConversionErrorCode.INPUT_PERMISSION_LOST,
        diagnostic = message,
        isRetryable = true,
    )
    ErrorCode.DESTINATION_NOT_WRITABLE -> ConversionError(
        ConversionErrorCode.OUTPUT_PERMISSION_LOST,
        diagnostic = message,
        isRetryable = true,
    )
    ErrorCode.INVALID_SELECTION -> ConversionError(ConversionErrorCode.INVALID_SELECTION, message)
    ErrorCode.MALFORMED_DOCUMENT -> ConversionError(ConversionErrorCode.INVALID_DOCUMENT, message)
    ErrorCode.ENCRYPTED_DOCUMENT -> ConversionError(ConversionErrorCode.PASSWORD_PROTECTED, message)
    ErrorCode.SECURITY_LIMIT_EXCEEDED -> ConversionError(ConversionErrorCode.UNSUPPORTED_INPUT, message)
    ErrorCode.OUT_OF_MEMORY -> ConversionError(ConversionErrorCode.OUT_OF_MEMORY, message, isRetryable = true)
    ErrorCode.IO_ERROR -> ConversionError(ConversionErrorCode.ENGINE_FAILURE, message, isRetryable = true)
    ErrorCode.INTERNAL_ERROR -> ConversionError(ConversionErrorCode.ENGINE_FAILURE, message)
}

internal fun buildOutputDisplayName(
    sourceDisplayName: String,
    direction: ConversionDirection,
    pattern: String,
    suffix: String = "",
): String {
    val sourceExtension = ".${direction.sourceExtension}"
    val baseName = if (sourceDisplayName.endsWith(sourceExtension, ignoreCase = true)) {
        sourceDisplayName.dropLast(sourceExtension.length)
    } else {
        sourceDisplayName.substringBeforeLast('.', sourceDisplayName)
    }.ifBlank { "converted" }
    val extension = direction.targetExtension
    var candidate = pattern
        .replace("{name}", baseName)
        .replace("{ext}", extension)
        .replace(Regex("[\\u0000-\\u001F\\\\/:*?\"<>|]"), "_")
        .trim()
        .ifBlank { baseName }
    if (suffix.isEmpty() && candidate.endsWith(".$extension", ignoreCase = true)) {
        return candidate
    }
    if (candidate.endsWith(".$extension", ignoreCase = true)) {
        candidate = candidate.dropLast(extension.length + 1)
    }
    candidate += suffix
    candidate += ".$extension"
    return candidate
}
