package com.converty.app.data.history

import com.converty.app.core.model.ConversionDocument
import com.converty.app.core.model.ConversionError
import com.converty.app.core.model.ConversionItem
import com.converty.app.core.model.ConversionJob
import com.converty.app.core.model.ConversionOptions
import com.converty.app.core.model.ConversionOutput
import com.converty.app.core.model.OutputOptions
import com.converty.app.core.model.selection.SelectionRequest
import com.converty.app.data.history.local.ConversionItemEntity
import com.converty.app.data.history.local.ConversionItemWithOutputs
import com.converty.app.data.history.local.ConversionJobEntity
import com.converty.app.data.history.local.ConversionJobWithItems
import com.converty.app.data.history.local.ConversionOutputEntity

private const val WARNING_SEPARATOR = '\u001F'

internal fun encodeWarningCodes(codes: List<String>): String? = codes
    .filter(String::isNotBlank)
    .distinct()
    .takeIf(List<String>::isNotEmpty)
    ?.joinToString(WARNING_SEPARATOR.toString())

internal fun ConversionJob.toEntity() = ConversionJobEntity(
    id = id,
    direction = direction,
    createdAtEpochMillis = createdAtEpochMillis,
    startedAtEpochMillis = startedAtEpochMillis,
    finishedAtEpochMillis = finishedAtEpochMillis,
    status = status,
    quality = options.quality,
    fit = options.fit,
    dpi = options.dpi,
    lossless = options.lossless,
    selectionMode = options.selection.mode,
    selectionCount = options.selection.count,
    selectionExpression = options.selection.expression,
    destinationTreeUri = options.output.destinationTreeUri,
    fileNamePattern = options.output.fileNamePattern,
    collisionPolicy = options.output.collisionPolicy,
    errorCode = error?.code,
    errorDiagnostic = error?.diagnostic,
    errorRetryable = error?.isRetryable ?: false,
)

internal fun ConversionItem.toEntity() = ConversionItemEntity(
    id = id,
    jobId = jobId,
    position = position,
    sourceUri = input.uri,
    sourceDisplayName = input.displayName,
    sourceMimeType = input.mimeType,
    sourceSizeBytes = input.sizeBytes,
    sourceLastModifiedEpochMillis = input.lastModifiedEpochMillis,
    sourceHasPersistedReadPermission = input.hasPersistedReadPermission,
    totalUnits = totalUnits,
    selectedUnits = selectedUnits,
    progressPercent = progressPercent,
    status = status,
    errorCode = error?.code,
    errorDiagnostic = error?.diagnostic,
    errorRetryable = error?.isRetryable ?: false,
    warningCodes = encodeWarningCodes(warningCodes),
)

internal fun ConversionOutput.toEntity(itemId: String, position: Int) = ConversionOutputEntity(
    itemId = itemId,
    position = position,
    uri = uri,
    displayName = displayName,
    mimeType = mimeType,
    sizeBytes = sizeBytes,
    createdAtEpochMillis = createdAtEpochMillis,
    isReadable = isReadable,
)

internal fun ConversionJobWithItems.toDomain(): ConversionJob {
    val options = ConversionOptions(
        selection = SelectionRequest(
            mode = job.selectionMode,
            count = job.selectionCount,
            expression = job.selectionExpression,
        ),
        quality = job.quality,
        fit = job.fit,
        dpi = job.dpi,
        lossless = job.lossless,
        output = OutputOptions(
            destinationTreeUri = job.destinationTreeUri,
            fileNamePattern = job.fileNamePattern,
            collisionPolicy = job.collisionPolicy,
        ),
    )
    return ConversionJob(
        id = job.id,
        direction = job.direction,
        createdAtEpochMillis = job.createdAtEpochMillis,
        startedAtEpochMillis = job.startedAtEpochMillis,
        finishedAtEpochMillis = job.finishedAtEpochMillis,
        status = job.status,
        options = options,
        items = items.sortedBy { it.item.position }.map(ConversionItemWithOutputs::toDomain),
        error = job.errorCode?.let { ConversionError(it, job.errorDiagnostic, job.errorRetryable) },
    )
}

private fun ConversionItemWithOutputs.toDomain() = ConversionItem(
    id = item.id,
    jobId = item.jobId,
    position = item.position,
    input = ConversionDocument(
        uri = item.sourceUri,
        displayName = item.sourceDisplayName,
        mimeType = item.sourceMimeType,
        sizeBytes = item.sourceSizeBytes,
        lastModifiedEpochMillis = item.sourceLastModifiedEpochMillis,
        hasPersistedReadPermission = item.sourceHasPersistedReadPermission,
    ),
    outputs = outputs.sortedBy(ConversionOutputEntity::position).map(ConversionOutputEntity::toDomain),
    totalUnits = item.totalUnits,
    selectedUnits = item.selectedUnits,
    progressPercent = item.progressPercent,
    status = item.status,
    error = item.errorCode?.let { ConversionError(it, item.errorDiagnostic, item.errorRetryable) },
    warningCodes = item.warningCodes
        ?.split(WARNING_SEPARATOR)
        ?.filter(String::isNotEmpty)
        .orEmpty(),
)

private fun ConversionOutputEntity.toDomain() = ConversionOutput(
    uri = uri,
    displayName = displayName,
    mimeType = mimeType,
    sizeBytes = sizeBytes,
    createdAtEpochMillis = createdAtEpochMillis,
    isReadable = isReadable,
)
