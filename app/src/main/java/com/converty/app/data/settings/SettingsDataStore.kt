package com.converty.app.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

private const val SETTINGS_FILE_NAME = "converty_settings"

val Context.convertySettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = SETTINGS_FILE_NAME,
)
