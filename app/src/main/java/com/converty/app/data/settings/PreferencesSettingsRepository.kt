package com.converty.app.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.converty.app.core.model.ContentFit
import com.converty.app.core.model.ConversionQuality
import com.converty.app.core.model.selection.SelectionMode
import com.converty.app.core.model.selection.SelectionRequest
import com.converty.app.core.repository.SettingsRepository
import com.converty.app.core.settings.AppLanguage
import com.converty.app.core.settings.AppSettings
import com.converty.app.core.settings.LanguagePreference
import com.converty.app.core.settings.ThemeMode
import com.converty.app.core.settings.ThemePalette
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class PreferencesSettingsRepository(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {
    override val settings: Flow<AppSettings> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map(::decode)

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        dataStore.edit { preferences ->
            encode(preferences, transform(decode(preferences)))
        }
    }

    override suspend fun reset() {
        dataStore.edit { it.clear() }
    }

    private fun decode(preferences: Preferences): AppSettings {
        val defaults = AppSettings()
        return AppSettings(
            themeMode = preferences[Keys.themeMode].toEnumOrDefault(defaults.themeMode),
            themePalette = preferences[Keys.themePalette].toEnumOrDefault(defaults.themePalette),
            useDynamicColor = preferences[Keys.dynamicColor] ?: defaults.useDynamicColor,
            language = preferences[Keys.languageTag]
                ?.takeIf(String::isNotBlank)
                ?.let { LanguagePreference.Specific(AppLanguage(it)) }
                ?: LanguagePreference.System,
            defaultQuality = resolveDefaultQuality(preferences),
            defaultFit = preferences[Keys.fit].toEnumOrDefault(defaults.defaultFit),
            defaultSelection = SelectionRequest(
                mode = preferences[Keys.selectionMode].toEnumOrDefault(SelectionMode.ALL),
                count = preferences[Keys.selectionCount],
                expression = preferences[Keys.selectionExpression],
            ),
            defaultOutputTreeUri = preferences[Keys.outputTreeUri],
            persistInputPermissions = preferences[Keys.persistInputPermissions]
                ?: defaults.persistInputPermissions,
            historyRetentionDays = (preferences[Keys.historyRetentionDays]
                ?: defaults.historyRetentionDays).coerceAtLeast(0),
        )
    }

    private fun encode(preferences: MutablePreferences, value: AppSettings) {
        preferences[Keys.themeMode] = value.themeMode.name
        preferences[Keys.themePalette] = value.themePalette.name
        preferences[Keys.dynamicColor] = value.useDynamicColor
        preferences[Keys.languageTag] = when (val language = value.language) {
            LanguagePreference.System -> ""
            is LanguagePreference.Specific -> language.language.languageTag
        }
        preferences[Keys.quality] = value.defaultQuality.name
        preferences[Keys.qualityDefaultVersion] = MAXIMUM_QUALITY_DEFAULT_VERSION
        preferences[Keys.fit] = value.defaultFit.name
        preferences[Keys.selectionMode] = value.defaultSelection.mode.name
        value.defaultSelection.count?.let { preferences[Keys.selectionCount] = it }
            ?: preferences.remove(Keys.selectionCount)
        value.defaultSelection.expression?.let { preferences[Keys.selectionExpression] = it }
            ?: preferences.remove(Keys.selectionExpression)
        value.defaultOutputTreeUri?.let { preferences[Keys.outputTreeUri] = it }
            ?: preferences.remove(Keys.outputTreeUri)
        preferences[Keys.persistInputPermissions] = value.persistInputPermissions
        preferences[Keys.historyRetentionDays] = value.historyRetentionDays
    }

    private inline fun <reified T : Enum<T>> String?.toEnumOrDefault(default: T): T =
        this?.let { value -> enumValues<T>().firstOrNull { it.name == value } } ?: default

    private object Keys {
        val themeMode = stringPreferencesKey("theme_mode")
        val themePalette = stringPreferencesKey("theme_palette")
        val dynamicColor = booleanPreferencesKey("dynamic_color")
        val languageTag = stringPreferencesKey("language_tag")
        val quality = stringPreferencesKey("default_quality")
        val qualityDefaultVersion = intPreferencesKey("quality_default_version")
        val fit = stringPreferencesKey("default_fit")
        val selectionMode = stringPreferencesKey("default_selection_mode")
        val selectionCount = intPreferencesKey("default_selection_count")
        val selectionExpression = stringPreferencesKey("default_selection_expression")
        val outputTreeUri = stringPreferencesKey("default_output_tree_uri")
        val persistInputPermissions = booleanPreferencesKey("persist_input_permissions")
        val historyRetentionDays = intPreferencesKey("history_retention_days")
    }

    companion object {
        private const val MAXIMUM_QUALITY_DEFAULT_VERSION = 1

        internal fun resolveDefaultQuality(preferences: Preferences): ConversionQuality {
            val version = preferences[Keys.qualityDefaultVersion] ?: 0
            if (version < MAXIMUM_QUALITY_DEFAULT_VERSION) return ConversionQuality.MAXIMUM

            val stored = preferences[Keys.quality]
            return stored?.let { value ->
                enumValues<ConversionQuality>().firstOrNull { it.name == value }
            } ?: ConversionQuality.MAXIMUM
        }
    }
}
