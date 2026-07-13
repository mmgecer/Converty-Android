package com.converty.app.work

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.converty.app.di.AppContainer

class ConversionWorkerFactory(
    private val appContainer: AppContainer,
) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
    ): ListenableWorker? = when (workerClassName) {
        ConversionWorker::class.java.name -> ConversionWorker(appContext, workerParameters, appContainer)
        else -> null
    }
}
