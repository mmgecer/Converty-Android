package com.converty.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.converty.app.core.settings.ThemePalette

internal val ConvertyLightColorScheme = lightColorScheme(
    primary = Color(0xFF405A91),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD8E2FF),
    onPrimaryContainer = Color(0xFF001A42),
    secondary = Color(0xFF565F71),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDAE2F9),
    onSecondaryContainer = Color(0xFF131C2B),
    tertiary = Color(0xFF705575),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFAD8FD),
    onTertiaryContainer = Color(0xFF28132E),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF9F9FF),
    onBackground = Color(0xFF191C20),
    surface = Color(0xFFF9F9FF),
    onSurface = Color(0xFF191C20),
    surfaceVariant = Color(0xFFE1E2EC),
    onSurfaceVariant = Color(0xFF44474F),
    outline = Color(0xFF74777F),
    outlineVariant = Color(0xFFC4C6D0),
    inverseSurface = Color(0xFF2E3036),
    inverseOnSurface = Color(0xFFF0F0F7),
    inversePrimary = Color(0xFFAFC6FF),
    surfaceTint = Color(0xFF405A91),
    scrim = Color(0xFF000000),
)

internal val ConvertyDarkColorScheme = darkColorScheme(
    primary = Color(0xFFAFC6FF),
    onPrimary = Color(0xFF0A2F60),
    primaryContainer = Color(0xFF274576),
    onPrimaryContainer = Color(0xFFD8E2FF),
    secondary = Color(0xFFBEC6DC),
    onSecondary = Color(0xFF283141),
    secondaryContainer = Color(0xFF3E4759),
    onSecondaryContainer = Color(0xFFDAE2F9),
    tertiary = Color(0xFFDDBCE0),
    onTertiary = Color(0xFF3F2845),
    tertiaryContainer = Color(0xFF573E5C),
    onTertiaryContainer = Color(0xFFFAD8FD),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF111318),
    onBackground = Color(0xFFE2E2E9),
    surface = Color(0xFF111318),
    onSurface = Color(0xFFE2E2E9),
    surfaceVariant = Color(0xFF44474F),
    onSurfaceVariant = Color(0xFFC4C6D0),
    outline = Color(0xFF8E9099),
    outlineVariant = Color(0xFF44474F),
    inverseSurface = Color(0xFFE2E2E9),
    inverseOnSurface = Color(0xFF2E3036),
    inversePrimary = Color(0xFF405A91),
    surfaceTint = Color(0xFFAFC6FF),
    scrim = Color(0xFF000000),
)

private val VioletLightColorScheme = ConvertyLightColorScheme.copy(
    primary = Color(0xFF6750A4),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    secondary = Color(0xFF625B71),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8DEF8),
    onSecondaryContainer = Color(0xFF1D192B),
    tertiary = Color(0xFF7D5260),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFD8E4),
    onTertiaryContainer = Color(0xFF31111D),
    surfaceTint = Color(0xFF6750A4),
)

private val VioletDarkColorScheme = ConvertyDarkColorScheme.copy(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFE8DEF8),
    tertiary = Color(0xFFEFB8C8),
    onTertiary = Color(0xFF492532),
    tertiaryContainer = Color(0xFF633B48),
    onTertiaryContainer = Color(0xFFFFD8E4),
    surfaceTint = Color(0xFFD0BCFF),
)

private val ForestLightColorScheme = ConvertyLightColorScheme.copy(
    primary = Color(0xFF386A20),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB7F397),
    onPrimaryContainer = Color(0xFF062100),
    secondary = Color(0xFF55624C),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD9E7CB),
    onSecondaryContainer = Color(0xFF131F0D),
    tertiary = Color(0xFF386666),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFBBEBEB),
    onTertiaryContainer = Color(0xFF002020),
    surfaceTint = Color(0xFF386A20),
)

private val ForestDarkColorScheme = ConvertyDarkColorScheme.copy(
    primary = Color(0xFF9CD67D),
    onPrimary = Color(0xFF123800),
    primaryContainer = Color(0xFF205107),
    onPrimaryContainer = Color(0xFFB7F397),
    secondary = Color(0xFFBDCBAF),
    onSecondary = Color(0xFF283420),
    secondaryContainer = Color(0xFF3E4A36),
    onSecondaryContainer = Color(0xFFD9E7CB),
    tertiary = Color(0xFFA0CFCF),
    onTertiary = Color(0xFF003737),
    tertiaryContainer = Color(0xFF1E4E4E),
    onTertiaryContainer = Color(0xFFBBEBEB),
    surfaceTint = Color(0xFF9CD67D),
)

private val SunsetLightColorScheme = ConvertyLightColorScheme.copy(
    primary = Color(0xFF8B4A00),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDCC2),
    onPrimaryContainer = Color(0xFF2D1600),
    secondary = Color(0xFF745A47),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDCC2),
    onSecondaryContainer = Color(0xFF2A180B),
    tertiary = Color(0xFF8B4A60),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFD9E2),
    onTertiaryContainer = Color(0xFF3A071D),
    surfaceTint = Color(0xFF8B4A00),
)

private val SunsetDarkColorScheme = ConvertyDarkColorScheme.copy(
    primary = Color(0xFFFFB77B),
    onPrimary = Color(0xFF4A2800),
    primaryContainer = Color(0xFF693900),
    onPrimaryContainer = Color(0xFFFFDCC2),
    secondary = Color(0xFFE3C1AA),
    onSecondary = Color(0xFF422B1B),
    secondaryContainer = Color(0xFF5B412F),
    onSecondaryContainer = Color(0xFFFFDCC2),
    tertiary = Color(0xFFFFB0C8),
    onTertiary = Color(0xFF541D32),
    tertiaryContainer = Color(0xFF703348),
    onTertiaryContainer = Color(0xFFFFD9E2),
    surfaceTint = Color(0xFFFFB77B),
)

internal fun colorSchemeFor(palette: ThemePalette, darkTheme: Boolean): ColorScheme = when (palette) {
    ThemePalette.OCEAN -> if (darkTheme) ConvertyDarkColorScheme else ConvertyLightColorScheme
    ThemePalette.VIOLET -> if (darkTheme) VioletDarkColorScheme else VioletLightColorScheme
    ThemePalette.FOREST -> if (darkTheme) ForestDarkColorScheme else ForestLightColorScheme
    ThemePalette.SUNSET -> if (darkTheme) SunsetDarkColorScheme else SunsetLightColorScheme
}

internal fun ThemePalette.previewColors(): List<Color> = when (this) {
    ThemePalette.OCEAN -> listOf(Color(0xFF405A91), Color(0xFF705575), Color(0xFFD8E2FF))
    ThemePalette.VIOLET -> listOf(Color(0xFF6750A4), Color(0xFF7D5260), Color(0xFFEADDFF))
    ThemePalette.FOREST -> listOf(Color(0xFF386A20), Color(0xFF386666), Color(0xFFB7F397))
    ThemePalette.SUNSET -> listOf(Color(0xFF8B4A00), Color(0xFF8B4A60), Color(0xFFFFDCC2))
}
