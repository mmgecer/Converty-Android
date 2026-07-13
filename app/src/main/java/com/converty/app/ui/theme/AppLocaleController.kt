package com.converty.app.ui.theme

import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.core.os.LocaleListCompat
import com.converty.app.core.settings.LanguagePreference

/**
 * Applies the persisted app language through AndroidX's supported per-app locale API.
 *
 * MainActivity declares locale/layoutDirection config changes, so this updates Compose resources
 * in place instead of recreating the Activity. The current-tag guard also prevents redundant
 * configuration dispatches during startup and recomposition.
 */
@Composable
fun ApplyAppLocale(preference: LanguagePreference) {
    val languageTags = when (preference) {
        LanguagePreference.System -> ""
        is LanguagePreference.Specific -> preference.language.languageTag
    }

    LaunchedEffect(languageTags) {
        if (AppCompatDelegate.getApplicationLocales().toLanguageTags() != languageTags) {
            runCatching {
                AppCompatDelegate.setApplicationLocales(
                    LocaleListCompat.forLanguageTags(languageTags),
                )
            }.onFailure { error ->
                Log.e("ConvertyStartup", "App locale could not be applied", error)
            }
        }
    }
}
