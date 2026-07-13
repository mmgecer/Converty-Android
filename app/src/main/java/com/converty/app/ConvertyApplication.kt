package com.converty.app

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import com.converty.app.di.AppContainer
import com.converty.app.work.ConversionNotifications

class ConvertyApplication : Application(), Configuration.Provider {
    /** Lazy initialization is safe even when WorkManager's startup provider runs before onCreate. */
    val appContainer: AppContainer by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        AppContainer(this)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(appContainer.workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        runCatching { ConversionNotifications.createChannel(this) }
            .onFailure { error ->
                Log.e("ConvertyStartup", "Notification channel could not be created", error)
            }
    }
}
