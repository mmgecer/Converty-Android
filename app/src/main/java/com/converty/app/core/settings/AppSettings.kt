package com.converty.app.core.settings

import com.converty.app.core.model.ContentFit
import com.converty.app.core.model.ConversionQuality
import com.converty.app.core.model.selection.SelectionRequest

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

enum class ThemePalette {
    OCEAN,
    VIOLET,
    FOREST,
    SUNSET,
}

/** BCP-47 tag. The open value keeps persistence compatible when a new locale is added. */
@JvmInline
value class AppLanguage(val languageTag: String) {
    init {
        require(languageTag.isNotBlank()) { "Language tag cannot be blank" }
    }
}

sealed interface LanguagePreference {
    data object System : LanguagePreference
    data class Specific(val language: AppLanguage) : LanguagePreference
}

object BuiltInAppLanguages {
    val English = AppLanguage("en")
    val Turkish = AppLanguage("tr")
    val German = AppLanguage("de")
    val SimplifiedChinese = AppLanguage("zh-Hans")
    val Arabic = AppLanguage("ar")
    val Portuguese = AppLanguage("pt")
    val French = AppLanguage("fr")
    val Russian = AppLanguage("ru")

    val all: List<AppLanguage> = listOf(
        English,
        Turkish,
        German,
        SimplifiedChinese,
        Arabic,
        Portuguese,
        French,
        Russian,
    )
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val themePalette: ThemePalette = ThemePalette.OCEAN,
    val useDynamicColor: Boolean = true,
    val language: LanguagePreference = LanguagePreference.System,
    val defaultQuality: ConversionQuality = ConversionQuality.MAXIMUM,
    val defaultFit: ContentFit = ContentFit.CONTAIN,
    val defaultSelection: SelectionRequest = SelectionRequest.all(),
    val defaultOutputTreeUri: String? = null,
    val persistInputPermissions: Boolean = true,
    val historyRetentionDays: Int = 90,
) {
    init {
        require(historyRetentionDays >= 0) { "History retention cannot be negative" }
    }
}
