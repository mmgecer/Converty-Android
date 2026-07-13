package com.converty.app.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Operation
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ConversionScheduler(context: Context) {
    private val appContext = context.applicationContext
    private val workManager by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        WorkManager.getInstance(appContext)
    }

    fun enqueue(jobId: String): UUID = schedule(jobId, ExistingWorkPolicy.KEEP)

    /** Replaces failed/cancelled work with a fresh attempt for the same persisted job. */
    fun retry(jobId: String): UUID = schedule(jobId, ExistingWorkPolicy.REPLACE)

    fun cancel(jobId: String): Operation = workManager.cancelUniqueWork(uniqueWorkName(jobId))

    fun observe(jobId: String): Flow<WorkInfo?> =
        workManager.getWorkInfosForUniqueWorkFlow(uniqueWorkName(jobId)).map(List<WorkInfo>::lastOrNull)

    private fun schedule(jobId: String, policy: ExistingWorkPolicy): UUID {
        require(jobId.isNotBlank()) { "Job id cannot be blank" }
        val request = OneTimeWorkRequestBuilder<ConversionWorker>()
            .setInputData(workDataOf(ConversionWorker.KEY_JOB_ID to jobId))
            .setConstraints(Constraints.Builder().setRequiresStorageNotLow(true).build())
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                MIN_BACKOFF_SECONDS,
                TimeUnit.SECONDS,
            )
            .addTag(TAG_ALL_CONVERSIONS)
            .addTag(jobTag(jobId))
            .build()
        workManager.enqueueUniqueWork(uniqueWorkName(jobId), policy, request)
        return request.id
    }

    companion object {
        private const val UNIQUE_WORK_PREFIX = "conversion-job-"
        private const val TAG_PREFIX = "conversion-job-tag-"
        private const val TAG_ALL_CONVERSIONS = "conversion-jobs"
        private const val MIN_BACKOFF_SECONDS = 10L

        fun uniqueWorkName(jobId: String): String = UNIQUE_WORK_PREFIX + jobId
        fun jobTag(jobId: String): String = TAG_PREFIX + jobId
    }
}
