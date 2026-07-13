package com.converty.app.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import com.converty.app.R
import java.util.UUID

object ConversionNotifications {
    private const val CHANNEL_ID = "conversion_progress"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.queue_title),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.overall_progress)
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    fun foregroundInfo(
        context: Context,
        workerId: UUID,
        jobId: String,
        completed: Int = 0,
        total: Int = 0,
    ): ForegroundInfo {
        createChannel(context)
        val boundedTotal = total.coerceAtLeast(0)
        val boundedCompleted = completed.coerceIn(0, maxOf(0, boundedTotal))
        val indeterminate = boundedTotal == 0
        val percent = if (indeterminate) 0 else (boundedCompleted * 100 / boundedTotal)
        val cancelIntent = WorkManager.getInstance(context).createCancelPendingIntent(workerId)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentTitle(context.getString(R.string.status_converting))
            .setContentText(
                if (indeterminate) {
                    context.getString(R.string.overall_progress)
                } else {
                    context.getString(R.string.progress_percent, percent)
                },
            )
            .setProgress(boundedTotal, boundedCompleted, indeterminate)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                context.getString(R.string.cancel),
                cancelIntent,
            )
            .build()

        val notificationId = notificationId(jobId)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                notificationId,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            ForegroundInfo(notificationId, notification)
        }
    }

    private fun notificationId(jobId: String): Int {
        val value = jobId.hashCode() and Int.MAX_VALUE
        return if (value == 0) 1 else value
    }
}
