package com.converty.app.data.history

import androidx.room.withTransaction
import com.converty.app.core.model.ConversionError
import com.converty.app.core.model.ConversionJob
import com.converty.app.core.model.ConversionOutput
import com.converty.app.core.model.ConversionStatus
import com.converty.app.core.repository.ConversionHistoryRepository
import com.converty.app.data.history.local.ConvertyDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomConversionHistoryRepository(
    private val database: ConvertyDatabase,
) : ConversionHistoryRepository {
    private val dao = database.conversionHistoryDao()

    override fun observeJobs(): Flow<List<ConversionJob>> =
        dao.observeJobs().map { jobs -> jobs.map { it.toDomain() } }

    override fun observeJob(jobId: String): Flow<ConversionJob?> =
        dao.observeJob(jobId).map { it?.toDomain() }

    override suspend fun getJob(jobId: String): ConversionJob? = dao.getJob(jobId)?.toDomain()

    override suspend fun saveSnapshot(job: ConversionJob) {
        database.withTransaction {
            dao.insertJob(job.toEntity())
            dao.deleteItemsForJob(job.id)
            if (job.items.isNotEmpty()) dao.insertItems(job.items.map { it.toEntity() })
            job.items.forEach { item ->
                if (item.outputs.isNotEmpty()) {
                    dao.insertOutputs(item.outputs.mapIndexed { index, output ->
                        output.toEntity(item.id, index)
                    })
                }
            }
        }
    }

    override suspend fun updateJobStatus(
        jobId: String,
        status: ConversionStatus,
        startedAtEpochMillis: Long?,
        finishedAtEpochMillis: Long?,
        error: ConversionError?,
    ) {
        dao.updateJobStatus(
            jobId = jobId,
            status = status,
            startedAtEpochMillis = startedAtEpochMillis,
            finishedAtEpochMillis = finishedAtEpochMillis,
            errorCode = error?.code,
            errorDiagnostic = error?.diagnostic,
            errorRetryable = error?.isRetryable ?: false,
        )
    }

    override suspend fun updateItemProgress(
        itemId: String,
        status: ConversionStatus,
        progressPercent: Int,
        totalUnits: Int?,
        selectedUnits: Int?,
        error: ConversionError?,
    ) {
        require(progressPercent in 0..100) { "Progress must be between 0 and 100" }
        dao.updateItemProgress(
            itemId = itemId,
            status = status,
            progressPercent = progressPercent,
            totalUnits = totalUnits,
            selectedUnits = selectedUnits,
            errorCode = error?.code,
            errorDiagnostic = error?.diagnostic,
            errorRetryable = error?.isRetryable ?: false,
        )
    }

    override suspend fun saveOutput(itemId: String, output: ConversionOutput) {
        dao.deleteOutputsForItem(itemId)
        dao.insertOutput(output.toEntity(itemId, 0))
    }

    override suspend fun saveOutputs(itemId: String, outputs: List<ConversionOutput>) {
        database.withTransaction {
            dao.deleteOutputsForItem(itemId)
            if (outputs.isNotEmpty()) {
                dao.insertOutputs(outputs.mapIndexed { index, output ->
                    output.toEntity(itemId, index)
                })
            }
        }
    }

    override suspend fun updateItemWarnings(itemId: String, warningCodes: List<String>) {
        dao.updateItemWarnings(itemId, encodeWarningCodes(warningCodes))
    }

    override suspend fun deleteHistoryRecord(jobId: String) = dao.deleteJob(jobId)

    override suspend fun clearHistory() = dao.clearHistory()
}
