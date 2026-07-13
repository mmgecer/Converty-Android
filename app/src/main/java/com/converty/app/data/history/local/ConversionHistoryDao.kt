package com.converty.app.data.history.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.converty.app.core.model.ConversionErrorCode
import com.converty.app.core.model.ConversionStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversionHistoryDao {
    @Transaction
    @Query("SELECT * FROM conversion_jobs ORDER BY created_at DESC")
    fun observeJobs(): Flow<List<ConversionJobWithItems>>

    @Transaction
    @Query("SELECT * FROM conversion_jobs WHERE id = :jobId LIMIT 1")
    fun observeJob(jobId: String): Flow<ConversionJobWithItems?>

    @Transaction
    @Query("SELECT * FROM conversion_jobs WHERE id = :jobId LIMIT 1")
    suspend fun getJob(jobId: String): ConversionJobWithItems?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJob(job: ConversionJobEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ConversionItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOutput(output: ConversionOutputEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOutputs(outputs: List<ConversionOutputEntity>)

    @Query("DELETE FROM conversion_outputs WHERE item_id = :itemId")
    suspend fun deleteOutputsForItem(itemId: String)

    @Query("DELETE FROM conversion_items WHERE job_id = :jobId")
    suspend fun deleteItemsForJob(jobId: String)

    @Query(
        """
        UPDATE conversion_jobs
        SET status = :status,
            started_at = :startedAtEpochMillis,
            finished_at = :finishedAtEpochMillis,
            error_code = :errorCode,
            error_diagnostic = :errorDiagnostic,
            error_retryable = :errorRetryable
        WHERE id = :jobId
        """,
    )
    suspend fun updateJobStatus(
        jobId: String,
        status: ConversionStatus,
        startedAtEpochMillis: Long?,
        finishedAtEpochMillis: Long?,
        errorCode: ConversionErrorCode?,
        errorDiagnostic: String?,
        errorRetryable: Boolean,
    )

    @Query(
        """
        UPDATE conversion_items
        SET status = :status,
            progress_percent = :progressPercent,
            total_units = :totalUnits,
            selected_units = :selectedUnits,
            error_code = :errorCode,
            error_diagnostic = :errorDiagnostic,
            error_retryable = :errorRetryable
        WHERE id = :itemId
        """,
    )
    suspend fun updateItemProgress(
        itemId: String,
        status: ConversionStatus,
        progressPercent: Int,
        totalUnits: Int?,
        selectedUnits: Int?,
        errorCode: ConversionErrorCode?,
        errorDiagnostic: String?,
        errorRetryable: Boolean,
    )

    @Query("UPDATE conversion_items SET warning_codes = :warningCodes WHERE id = :itemId")
    suspend fun updateItemWarnings(itemId: String, warningCodes: String?)

    @Query("DELETE FROM conversion_jobs WHERE id = :jobId")
    suspend fun deleteJob(jobId: String)

    @Query("DELETE FROM conversion_jobs")
    suspend fun clearHistory()
}
