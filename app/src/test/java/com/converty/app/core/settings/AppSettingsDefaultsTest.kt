package com.converty.app.core.settings

import com.converty.app.core.model.ConversionQuality
import com.converty.app.core.model.ConversionDirection
import com.converty.app.feature.app.ConversionSetupState
import org.junit.Assert.assertEquals
import org.junit.Test

class AppSettingsDefaultsTest {
    @Test
    fun `new settings use maximum conversion quality and ocean palette`() {
        val settings = AppSettings()

        assertEquals(ConversionQuality.MAXIMUM, settings.defaultQuality)
        assertEquals(ThemePalette.OCEAN, settings.themePalette)
        assertEquals(4, ThemePalette.entries.size)
        assertEquals(
            ConversionQuality.MAXIMUM,
            ConversionSetupState(ConversionDirection.PDF_TO_PPTX).quality,
        )
    }
}
