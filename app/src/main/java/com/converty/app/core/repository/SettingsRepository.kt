package com.converty.app.core.repository

import com.converty.app.core.settings.AppSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun update(transform: (AppSettings) -> AppSettings)
    suspend fun reset()
}
