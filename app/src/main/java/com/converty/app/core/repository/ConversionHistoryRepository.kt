package com.converty.app.core.repository

import com.converty.app.core.model.ConversionError
import com.converty.app.core.model.ConversionJob
import com.converty.app.core.model.ConversionOutput
import com.converty.app.core.model.ConversionStatus
import kotlinx.coroutines.flow.Flow

interface ConversionHistoryRepository {
    fun observeJobs(): Flow<List<ConversionJob>>
    fun observeJob(jobId: String): Flow<ConversionJob?>
    suspend fun getJob(jobId: String): ConversionJob?

    /** Replaces a complete job snapshot, including its items and outputs. */
    suspend fun saveSnapshot(job: ConversionJob)

    suspend fun updateJobStatus(
        jobId: String,
        status: ConversionStatus,
        startedAtEpochMillis: Long? = null,
        finishedAtEpochMillis: Long? = null,
        error: ConversionError? = null,
    )

    suspend fun updateItemProgress(
        itemId: String,
        status: ConversionStatus,
        progressPercent: Int,
        totalUnits: Int? = null,
        selectedUnits: Int? = null,
        error: ConversionError? = null,
    )

    suspend fun updateItemWarnings(itemId: String, warningCodes: List<String>)
    suspend fun saveOutput(itemId: String, output: ConversionOutput)
    suspend fun saveOutputs(itemId: String, outputs: List<ConversionOutput>)
    suspend fun deleteHistoryRecord(jobId: String)
    suspend fun clearHistory()
}
