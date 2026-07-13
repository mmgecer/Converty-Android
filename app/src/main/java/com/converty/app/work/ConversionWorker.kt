package com.converty.app.work

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.converty.app.core.conversion.ConversionEngine
import com.converty.app.core.conversion.ConversionProgress
import com.converty.app.core.conversion.ConversionRequest
import com.converty.app.core.conversion.ConversionResult
import com.converty.app.core.conversion.ConversionWarning
import com.converty.app.core.conversion.DocumentInspectionResult
import com.converty.app.core.conversion.IndexedDestination
import com.converty.app.core.conversion.MultiOutputConversionEngine
import com.converty.app.core.conversion.MultiOutputConversionRequest
import com.converty.app.core.conversion.MultiOutputConversionResult
import com.converty.app.core.model.ConversionDirection
import com.converty.app.core.model.ConversionError
import com.converty.app.core.model.ConversionErrorCode
import com.converty.app.core.model.ConversionItem
import com.converty.app.core.model.ConversionJob
import com.converty.app.core.model.ConversionOutput
import com.converty.app.core.model.ConversionStatus
import com.converty.app.core.model.NameCollisionPolicy
import com.converty.app.data.files.OutputAlreadyExistsException
import com.converty.app.di.AppContainer
import java.io.FileNotFoundException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ConversionWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
    private val appContainer: AppContainer,
) : CoroutineWorker(appContext, workerParameters) {
    private val history = appContainer.historyRepository

    private val workerCreatedOutputUris = linkedSetOf<Uri>()

    @Volatile
    private var activeItemId: String? = null

    override suspend fun doWork(): Result {
        val jobId = inputData.getString(KEY_JOB_ID)
            ?.takeIf(String::isNotBlank)
            ?: return Result.failure(workDataOf(KEY_ERROR_CODE to "missing_job_id"))
        setForeground(ConversionNotifications.foregroundInfo(applicationContext, id, jobId))

        return try {
            executeJob(jobId)
        } catch (cancelled: CancellationException) {
            withContext(NonCancellable + Dispatchers.IO) {
                cleanupWorkerCreatedOutput()
                markCancelled(jobId)
            }
            throw cancelled
        } catch (error: Throwable) {
            val domainError = unexpectedError(error)
            withContext(NonCancellable + Dispatchers.IO) {
                cleanupWorkerCreatedOutput()
                activeItemId?.let { itemId ->
                    val item = history.getJob(jobId)?.items?.firstOrNull { it.id == itemId }
                    history.updateItemProgress(
                        itemId = itemId,
                        status = ConversionStatus.FAILED,
                        progressPercent = item?.progressPercent ?: 0,
                        totalUnits = item?.totalUnits,
                        selectedUnits = item?.selectedUnits,
                        error = domainError,
                    )
                }
                history.updateJobStatus(
                    jobId = jobId,
                    status = ConversionStatus.FAILED,
                    startedAtEpochMillis = history.getJob(jobId)?.startedAtEpochMillis,
                    finishedAtEpochMillis = System.currentTimeMillis(),
                    error = domainError,
                )
            }
            Result.failure(
                workDataOf(KEY_JOB_ID to jobId, KEY_ERROR_CODE to domainError.code.name),
            )
        }
    }

    private suspend fun executeJob(jobId: String): Result {
        val initialJob = history.getJob(jobId)
            ?: return Result.failure(
                workDataOf(KEY_JOB_ID to jobId, KEY_ERROR_CODE to "job_not_found"),
            )
        if (initialJob.items.isEmpty()) {
            val error = ConversionError(ConversionErrorCode.INPUT_NOT_FOUND, "Conversion job has no items")
            history.updateJobStatus(
                jobId,
                ConversionStatus.FAILED,
                initialJob.startedAtEpochMillis,
                System.currentTimeMillis(),
                error,
            )
            return Result.failure(workDataOf(KEY_JOB_ID to jobId, KEY_ERROR_CODE to error.code.name))
        }

        val startedAt = initialJob.startedAtEpochMillis ?: System.currentTimeMillis()
        history.updateJobStatus(
            jobId = jobId,
            status = ConversionStatus.RUNNING,
            startedAtEpochMillis = startedAt,
            finishedAtEpochMillis = null,
            error = null,
        )

        initialJob.items.sortedBy(ConversionItem::position).forEachIndexed { index, item ->
            if (isStopped) throw CancellationException("Conversion work was stopped")
            if (item.status.isCompletedSuccessfully && item.outputs.isNotEmpty()) return@forEachIndexed
            activeItemId = item.id
            setForeground(
                ConversionNotifications.foregroundInfo(
                    applicationContext,
                    id,
                    jobId,
                    completed = index,
                    total = initialJob.items.size,
                ),
            )
            processItem(initialJob, item)
        }
        activeItemId = null

        val finalJob = history.getJob(jobId) ?: return Result.failure()
        val failedItems = finalJob.items.filter { it.status == ConversionStatus.FAILED }
        val cancelledItems = finalJob.items.filter { it.status == ConversionStatus.CANCELLED }
        val firstFailure = failedItems.firstNotNullOfOrNull(ConversionItem::error)
        if (failedItems.isNotEmpty()) {
            val shouldRetry = runAttemptCount < MAX_AUTOMATIC_RETRIES &&
                failedItems.all { it.error?.isRetryable == true }
            history.updateJobStatus(
                jobId = jobId,
                status = if (shouldRetry) ConversionStatus.QUEUED else ConversionStatus.FAILED,
                startedAtEpochMillis = startedAt,
                finishedAtEpochMillis = if (shouldRetry) null else System.currentTimeMillis(),
                error = firstFailure,
            )
            return if (shouldRetry) {
                Result.retry()
            } else {
                Result.failure(
                    workDataOf(
                        KEY_JOB_ID to jobId,
                        KEY_ERROR_CODE to (firstFailure?.code?.name ?: "item_failed"),
                    ),
                )
            }
        }
        if (cancelledItems.isNotEmpty()) {
            history.updateJobStatus(
                jobId,
                ConversionStatus.CANCELLED,
                startedAt,
                System.currentTimeMillis(),
                ConversionError(ConversionErrorCode.CANCELLED),
            )
            return Result.failure(workDataOf(KEY_JOB_ID to jobId, KEY_ERROR_CODE to "cancelled"))
        }

        val hasWarnings = finalJob.items.any {
            it.warningCodes.isNotEmpty() || it.status == ConversionStatus.SUCCEEDED_WITH_WARNINGS
        }
        val finalStatus = if (hasWarnings) {
            ConversionStatus.SUCCEEDED_WITH_WARNINGS
        } else {
            ConversionStatus.SUCCEEDED
        }
        history.updateJobStatus(
            jobId,
            finalStatus,
            startedAt,
            System.currentTimeMillis(),
            null,
        )
        setForeground(
            ConversionNotifications.foregroundInfo(
                applicationContext,
                id,
                jobId,
                completed = finalJob.items.size,
                total = finalJob.items.size,
            ),
        )
        return Result.success(workDataOf(KEY_JOB_ID to jobId))
    }

    private suspend fun processItem(job: ConversionJob, item: ConversionItem) {
        history.updateItemWarnings(item.id, emptyList())
        history.updateItemProgress(
            item.id,
            ConversionStatus.PREPARING,
            0,
            item.totalUnits,
            null,
            null,
        )
        val engine = engineFor(job.direction)
        val inspection = try {
            withContext(Dispatchers.IO) {
                engine.inspect(Uri.parse(item.input.uri), cancellation = { isStopped })
            }
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            val domainError = unexpectedError(error)
            history.updateItemProgress(
                item.id,
                ConversionStatus.FAILED,
                0,
                item.totalUnits,
                null,
                domainError,
            )
            return
        }
        val inspectedItem: ConversionItem
        val inspectionWarningCodes: List<String>
        when (inspection) {
            is DocumentInspectionResult.Success -> {
                if (inspection.inspection.itemCount <= 0) {
                    history.updateItemProgress(
                        item.id,
                        ConversionStatus.FAILED,
                        0,
                        0,
                        null,
                        ConversionError(
                            ConversionErrorCode.INVALID_DOCUMENT,
                            "Document contains no pages or slides",
                        ),
                    )
                    return
                }
                inspectedItem = item.copy(totalUnits = inspection.inspection.itemCount)
                inspectionWarningCodes = inspection.inspection.warnings.map(ConversionWarning::code)
                    .filter(String::isNotBlank)
                    .distinct()
                history.updateItemWarnings(item.id, inspectionWarningCodes)
                history.updateItemProgress(
                    item.id,
                    ConversionStatus.PREPARING,
                    0,
                    inspectedItem.totalUnits,
                    null,
                    null,
                )
            }
            is DocumentInspectionResult.Failure -> {
                history.updateItemProgress(
                    item.id,
                    ConversionStatus.FAILED,
                    0,
                    item.totalUnits,
                    null,
                    inspection.code.toDomainError(
                        inspection.message.take(MAX_DIAGNOSTIC_LENGTH),
                    ),
                )
                return
            }
            DocumentInspectionResult.Cancelled -> {
                history.updateItemProgress(
                    item.id,
                    ConversionStatus.CANCELLED,
                    0,
                    item.totalUnits,
                    null,
                    ConversionError(ConversionErrorCode.CANCELLED),
                )
                return
            }
        }

        val selection = resolveWorkerSelection(inspectedItem, job.options)
        if (selection is WorkerSelectionResolution.Failure) {
            val diagnostic = selection.error?.code?.name ?: "Item count is required for this selection mode"
            history.updateItemProgress(
                item.id,
                ConversionStatus.FAILED,
                0,
                inspectedItem.totalUnits,
                null,
                ConversionError(ConversionErrorCode.INVALID_SELECTION, diagnostic),
            )
            return
        }
        selection as WorkerSelectionResolution.Success
        history.updateItemProgress(
            item.id,
            ConversionStatus.PREPARING,
            0,
            inspectedItem.totalUnits,
            selection.selectedCount,
            null,
        )

        if (engine is MultiOutputConversionEngine) {
            processMultiOutput(
                job = job,
                item = inspectedItem,
                selection = selection,
                inspectionWarningCodes = inspectionWarningCodes,
                engine = engine,
            )
            return
        }

        val destination = try {
            prepareDestination(job, inspectedItem)
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            val domainError = ConversionError(
                code = if (error is SecurityException) {
                    ConversionErrorCode.OUTPUT_PERMISSION_LOST
                } else {
                    ConversionErrorCode.OUTPUT_CREATE_FAILED
                },
                diagnostic = error.message?.take(MAX_DIAGNOSTIC_LENGTH),
                isRetryable = error is FileNotFoundException || error is SecurityException,
            )
            history.updateItemProgress(
                item.id,
                ConversionStatus.FAILED,
                0,
                inspectedItem.totalUnits,
                selection.selectedCount,
                domainError,
            )
            return
        }

        val request = ConversionRequest(
            source = Uri.parse(item.input.uri),
            destination = destination.uri,
            selectedItemIndices = selection.zeroBasedIndices,
            imageQuality = job.options.quality.toEngineQuality(),
            imageFit = job.options.fit.toEngineFit(),
            dpi = job.options.dpi,
            lossless = job.options.lossless,
        )
        val result = runEngine(
            jobId = job.id,
            item = inspectedItem,
            selectedCount = selection.selectedCount,
            engine = engine,
            request = request,
        )
        persistResult(job, inspectedItem, destination, inspectionWarningCodes, result)
    }

    private suspend fun processMultiOutput(
        job: ConversionJob,
        item: ConversionItem,
        selection: WorkerSelectionResolution.Success,
        inspectionWarningCodes: List<String>,
        engine: MultiOutputConversionEngine,
    ) {
        val total = item.totalUnits ?: return
        val itemIndices = selection.zeroBasedIndices.ifEmpty { (0 until total).toList() }
        val digits = maxOf(3, total.toString().length)
        val destinations = try {
            itemIndices.mapIndexed { outputPosition, itemIndex ->
                prepareDestination(
                    job = job,
                    item = item,
                    suffix = "-page-${(itemIndex + 1).toString().padStart(digits, '0')}",
                    existingOutput = item.outputs.getOrNull(outputPosition),
                )
            }
        } catch (error: Throwable) {
            cleanupWorkerCreatedOutput()
            history.updateItemProgress(
                item.id,
                ConversionStatus.FAILED,
                0,
                total,
                itemIndices.size,
                ConversionError(
                    ConversionErrorCode.OUTPUT_CREATE_FAILED,
                    diagnostic = error.message?.take(MAX_DIAGNOSTIC_LENGTH),
                    isRetryable = error is FileNotFoundException || error is SecurityException,
                ),
            )
            return
        }
        val request = MultiOutputConversionRequest(
            source = Uri.parse(item.input.uri),
            destinations = itemIndices.mapIndexed { index, itemIndex ->
                IndexedDestination(itemIndex, destinations[index].uri)
            },
            imageQuality = job.options.quality.toEngineQuality(),
            dpi = job.options.dpi,
        )
        val result = runMultiOutputEngine(
            jobId = job.id,
            item = item,
            selectedCount = itemIndices.size,
            engine = engine,
            request = request,
        )
        when (result) {
            is MultiOutputConversionResult.Success -> {
                val warningCodes = (inspectionWarningCodes + result.warnings.map(ConversionWarning::code))
                    .filter(String::isNotBlank)
                    .distinct()
                val outputs = result.outputs.mapIndexed { position, produced ->
                    val prepared = destinations.first { it.uri == produced.uri }
                    val metadata = runCatching {
                        appContainer.safDocumentGateway.metadata(produced.uri)
                    }.getOrNull()
                    ConversionOutput(
                        uri = produced.uri.toString(),
                        displayName = metadata?.displayName ?: prepared.displayName,
                        mimeType = metadata?.mimeType ?: job.direction.targetMimeType,
                        sizeBytes = metadata?.sizeBytes ?: produced.bytesWritten,
                        createdAtEpochMillis = item.outputs.getOrNull(position)?.createdAtEpochMillis
                            ?: System.currentTimeMillis(),
                        isReadable = true,
                    )
                }
                history.saveOutputs(item.id, outputs)
                history.updateItemWarnings(item.id, warningCodes)
                history.updateItemProgress(
                    item.id,
                    if (warningCodes.isEmpty()) ConversionStatus.SUCCEEDED else ConversionStatus.SUCCEEDED_WITH_WARNINGS,
                    100,
                    total,
                    outputs.size,
                    null,
                )
                destinations.forEach { workerCreatedOutputUris.remove(it.uri) }
            }
            is MultiOutputConversionResult.Failure -> {
                cleanupWorkerCreatedOutput()
                history.updateItemProgress(
                    item.id,
                    ConversionStatus.FAILED,
                    0,
                    total,
                    itemIndices.size,
                    result.code.toDomainError(result.message.take(MAX_DIAGNOSTIC_LENGTH)),
                )
            }
            is MultiOutputConversionResult.Unsupported -> {
                cleanupWorkerCreatedOutput()
                val warnings = (inspectionWarningCodes + result.warnings.map(ConversionWarning::code))
                    .filter(String::isNotBlank)
                    .distinct()
                history.updateItemWarnings(item.id, warnings)
                history.updateItemProgress(
                    item.id,
                    ConversionStatus.FAILED,
                    0,
                    total,
                    itemIndices.size,
                    ConversionError(
                        ConversionErrorCode.UNSUPPORTED_INPUT,
                        diagnostic = result.features.joinToString(", ").take(MAX_DIAGNOSTIC_LENGTH),
                    ),
                )
            }
            MultiOutputConversionResult.Cancelled -> {
                cleanupWorkerCreatedOutput()
                history.updateItemProgress(
                    item.id,
                    ConversionStatus.CANCELLED,
                    0,
                    total,
                    itemIndices.size,
                    ConversionError(ConversionErrorCode.CANCELLED),
                )
            }
        }
    }

    private suspend fun runMultiOutputEngine(
        jobId: String,
        item: ConversionItem,
        selectedCount: Int,
        engine: MultiOutputConversionEngine,
        request: MultiOutputConversionRequest,
    ): MultiOutputConversionResult = coroutineScope {
        val updates = Channel<ConversionProgress>(Channel.CONFLATED)
        val updater = launch {
            for (progress in updates) {
                val percent = if (progress.totalItems <= 0) 0 else {
                    (progress.completedItems * 100 / progress.totalItems).coerceIn(0, 100)
                }
                history.updateItemProgress(
                    item.id,
                    ConversionStatus.RUNNING,
                    percent,
                    item.totalUnits,
                    selectedCount,
                    null,
                )
                setProgress(workDataOf(KEY_JOB_ID to jobId, KEY_ITEM_ID to item.id, KEY_PROGRESS_PERCENT to percent))
            }
        }
        try {
            withContext(Dispatchers.IO) {
                engine.convertMultiple(request, cancellation = { isStopped }) { updates.trySend(it) }
            }
        } finally {
            updates.close()
            updater.join()
        }
    }

    private suspend fun runEngine(
        jobId: String,
        item: ConversionItem,
        selectedCount: Int?,
        engine: ConversionEngine,
        request: ConversionRequest,
    ): ConversionResult = coroutineScope {
        val updates = Channel<ConversionProgress>(Channel.CONFLATED)
        val updater = launch {
            for (progress in updates) {
                val percent = if (progress.totalItems <= 0) {
                    0
                } else {
                    (progress.completedItems * 100 / progress.totalItems).coerceIn(0, 100)
                }
                history.updateItemProgress(
                    item.id,
                    ConversionStatus.RUNNING,
                    percent,
                    item.totalUnits,
                    selectedCount ?: progress.totalItems.takeIf { it > 0 },
                    null,
                )
                setProgress(
                    workDataOf(
                        KEY_JOB_ID to jobId,
                        KEY_ITEM_ID to item.id,
                        KEY_PROGRESS_PERCENT to percent,
                        KEY_PROGRESS_STAGE to progress.stage.name,
                    ),
                )
                setForeground(
                    ConversionNotifications.foregroundInfo(
                        applicationContext,
                        id,
                        jobId,
                        progress.completedItems,
                        progress.totalItems,
                    ),
                )
            }
        }
        try {
            withContext(Dispatchers.IO) {
                engine.convert(
                    request = request,
                    cancellation = { isStopped },
                    onProgress = { updates.trySend(it) },
                )
            }
        } finally {
            updates.close()
            updater.join()
        }
    }

    private suspend fun persistResult(
        job: ConversionJob,
        item: ConversionItem,
        destination: PreparedDestination,
        inspectionWarningCodes: List<String>,
        result: ConversionResult,
    ) {
        when (result) {
            is ConversionResult.Success -> {
                val warningCodes = (inspectionWarningCodes + result.warnings.map(ConversionWarning::code))
                    .filter(String::isNotBlank)
                    .distinct()
                val metadata = runCatching { appContainer.safDocumentGateway.metadata(result.output) }.getOrNull()
                history.saveOutput(
                    item.id,
                    ConversionOutput(
                        uri = result.output.toString(),
                        displayName = metadata?.displayName ?: destination.displayName,
                        mimeType = metadata?.mimeType ?: job.direction.targetMimeType,
                        sizeBytes = metadata?.sizeBytes ?: result.bytesWritten,
                        createdAtEpochMillis = item.output?.createdAtEpochMillis ?: System.currentTimeMillis(),
                        isReadable = true,
                    ),
                )
                history.updateItemWarnings(item.id, warningCodes)
                history.updateItemProgress(
                    item.id,
                    if (warningCodes.isEmpty()) {
                        ConversionStatus.SUCCEEDED
                    } else {
                        ConversionStatus.SUCCEEDED_WITH_WARNINGS
                    },
                    100,
                    item.totalUnits,
                    result.convertedItems,
                    null,
                )
                workerCreatedOutputUris.remove(result.output)
            }
            is ConversionResult.Failure -> {
                cleanupWorkerCreatedOutput()
                history.updateItemProgress(
                    item.id,
                    ConversionStatus.FAILED,
                    0,
                    item.totalUnits,
                    null,
                    result.code.toDomainError(result.message.take(MAX_DIAGNOSTIC_LENGTH)),
                )
            }
            is ConversionResult.Unsupported -> {
                val warningCodes = (inspectionWarningCodes + result.warnings.map(ConversionWarning::code))
                    .filter(String::isNotBlank)
                    .distinct()
                    .ifEmpty { listOf("unsupported_pptx_content") }
                cleanupWorkerCreatedOutput()
                history.updateItemWarnings(item.id, warningCodes)
                history.updateItemProgress(
                    item.id,
                    ConversionStatus.FAILED,
                    0,
                    item.totalUnits,
                    null,
                    ConversionError(
                        ConversionErrorCode.UNSUPPORTED_INPUT,
                        diagnostic = result.features.joinToString(", ").take(MAX_DIAGNOSTIC_LENGTH),
                    ),
                )
            }
            ConversionResult.Cancelled -> {
                cleanupWorkerCreatedOutput()
                history.updateItemProgress(
                    item.id,
                    ConversionStatus.CANCELLED,
                    0,
                    item.totalUnits,
                    null,
                    ConversionError(ConversionErrorCode.CANCELLED),
                )
            }
        }
    }

    private fun prepareDestination(
        job: ConversionJob,
        item: ConversionItem,
        suffix: String = "",
        existingOutput: ConversionOutput? = item.output,
    ): PreparedDestination {
        existingOutput?.let { existing ->
            return PreparedDestination(Uri.parse(existing.uri), existing.displayName)
        }
        val tree = job.options.output.destinationTreeUri
            ?.takeIf(String::isNotBlank)
            ?: throw FileNotFoundException("No output folder is associated with the conversion job")
        val displayName = buildOutputDisplayName(
            item.input.displayName,
            job.direction,
            job.options.output.fileNamePattern,
            suffix,
        )
        val existing = appContainer.safDocumentGateway.findOutput(Uri.parse(tree), displayName)
        when (job.options.output.collisionPolicy) {
            NameCollisionPolicy.REPLACE -> if (existing != null) {
                return PreparedDestination(existing.uri, displayName)
            }
            NameCollisionPolicy.FAIL -> if (existing != null) {
                throw OutputAlreadyExistsException(displayName)
            }
            NameCollisionPolicy.CREATE_UNIQUE -> Unit
        }
        val handle = appContainer.safDocumentGateway.createOutput(
            Uri.parse(tree),
            job.direction.targetMimeType,
            displayName,
        )
        workerCreatedOutputUris += handle.uri
        return PreparedDestination(handle.uri, displayName)
    }

    private fun engineFor(direction: ConversionDirection): ConversionEngine = when (direction) {
        ConversionDirection.PDF_TO_PPTX -> appContainer.pdfToPptxEngine
        ConversionDirection.PPTX_TO_PDF -> appContainer.pptxToPdfEngine
        ConversionDirection.PDF_TO_PNG -> appContainer.pdfToPngEngine
        ConversionDirection.PDF_TO_JPG -> appContainer.pdfToJpgEngine
        ConversionDirection.PDF_TO_TIFF -> appContainer.pdfToTiffEngine
        ConversionDirection.PDF_TO_THUMBNAIL -> appContainer.pdfThumbnailEngine
        else -> appContainer.imageTranscodeEngine(direction)
            ?: appContainer.officeToPdfEngine(direction.sourceFormat)
            ?: error("No conversion engine is registered for ${direction.name}")
    }

    private suspend fun cleanupWorkerCreatedOutput() {
        val uris = workerCreatedOutputUris.toList()
        if (uris.isEmpty()) return
        workerCreatedOutputUris.clear()
        withContext(Dispatchers.IO) {
            uris.forEach { uri ->
                runCatching { appContainer.safDocumentGateway.output(uri).delete() }
            }
        }
    }

    private suspend fun markCancelled(jobId: String) {
        val job = history.getJob(jobId) ?: return
        job.items.filterNot { it.status.isTerminal }.forEach { item ->
            history.updateItemProgress(
                item.id,
                ConversionStatus.CANCELLED,
                item.progressPercent,
                item.totalUnits,
                item.selectedUnits,
                ConversionError(ConversionErrorCode.CANCELLED),
            )
        }
        history.updateJobStatus(
            jobId,
            ConversionStatus.CANCELLED,
            job.startedAtEpochMillis,
            System.currentTimeMillis(),
            ConversionError(ConversionErrorCode.CANCELLED),
        )
    }

    private fun unexpectedError(error: Throwable): ConversionError = when (error) {
        is OutOfMemoryError -> ConversionError(
            ConversionErrorCode.OUT_OF_MEMORY,
            diagnostic = "Conversion ran out of memory",
            isRetryable = true,
        )
        is SecurityException -> ConversionError(
            ConversionErrorCode.INPUT_PERMISSION_LOST,
            diagnostic = error.message?.take(MAX_DIAGNOSTIC_LENGTH),
            isRetryable = true,
        )
        else -> ConversionError(
            ConversionErrorCode.UNKNOWN,
            diagnostic = error.message?.take(MAX_DIAGNOSTIC_LENGTH),
            isRetryable = error is java.io.IOException,
        )
    }

    private data class PreparedDestination(val uri: Uri, val displayName: String)

    companion object {
        const val KEY_JOB_ID = "job_id"
        const val KEY_ITEM_ID = "item_id"
        const val KEY_PROGRESS_PERCENT = "progress_percent"
        const val KEY_PROGRESS_STAGE = "progress_stage"
        const val KEY_ERROR_CODE = "error_code"
        private const val MAX_AUTOMATIC_RETRIES = 2
        private const val MAX_DIAGNOSTIC_LENGTH = 300
    }
}

private val ConversionStatus.isCompletedSuccessfully: Boolean
    get() = this == ConversionStatus.SUCCEEDED || this == ConversionStatus.SUCCEEDED_WITH_WARNINGS

private val ConversionStatus.isTerminal: Boolean
    get() = isCompletedSuccessfully || this == ConversionStatus.FAILED || this == ConversionStatus.CANCELLED
