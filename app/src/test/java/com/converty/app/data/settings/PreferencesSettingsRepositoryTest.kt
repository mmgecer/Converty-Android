package com.converty.app.data.settings

import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.converty.app.core.model.ConversionQuality
import org.junit.Assert.assertEquals
import org.junit.Test

class PreferencesSettingsRepositoryTest {
    @Test
    fun `legacy quality resolves to maximum without a startup write`() {
        val qualityKey = stringPreferencesKey("default_quality")
        val migrationVersionKey = intPreferencesKey("quality_default_version")
        val preferences = mutablePreferencesOf(
            qualityKey to ConversionQuality.BALANCED.name,
        )

        assertEquals(
            ConversionQuality.MAXIMUM,
            PreferencesSettingsRepository.resolveDefaultQuality(preferences),
        )
        assertEquals(ConversionQuality.BALANCED.name, preferences[qualityKey])
        assertEquals(null, preferences[migrationVersionKey])

        preferences[migrationVersionKey] = 1
        preferences[qualityKey] = ConversionQuality.COMPACT.name
        assertEquals(
            ConversionQuality.COMPACT,
            PreferencesSettingsRepository.resolveDefaultQuality(preferences),
        )
    }
}
