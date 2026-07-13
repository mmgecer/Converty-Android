package com.converty.app.ui.theme

import androidx.annotation.StringRes
import com.converty.app.R

data class SupportedAppLocale(
    val languageTag: String,
    @param:StringRes val displayNameRes: Int,
)

/** Add a resource directory, locale_config entry and item here when adding a language. */
object SupportedAppLocales {
    val All: List<SupportedAppLocale> = listOf(
        SupportedAppLocale("en", R.string.language_english),
        SupportedAppLocale("tr", R.string.language_turkish),
        SupportedAppLocale("de", R.string.language_german),
        SupportedAppLocale("zh-Hans", R.string.language_chinese_simplified),
        SupportedAppLocale("ar", R.string.language_arabic),
        SupportedAppLocale("pt", R.string.language_portuguese),
        SupportedAppLocale("fr", R.string.language_french),
        SupportedAppLocale("ru", R.string.language_russian),
    )
}
